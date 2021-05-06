package net.querz.mcaselector.io.job;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.ByteArrayReader;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileImage;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RegionImageGenerator {

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();
	private static final Set<Point2i> saving = ConcurrentHashMap.newKeySet();
	private static final Map<Point2i, Runnable> onSaved = new ConcurrentHashMap<>();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
		MCAFilePipe.addJob(new MCAImageLoadJob(tile, world, callback, scaleSupplier, scaleOnly, progressChannel));
	}

	public static boolean isLoading(Point2i region) {
		return loading.contains(region);
	}

	public static void setLoading(Point2i region, boolean loading) {
		if (loading) {
			RegionImageGenerator.loading.add(region);
		} else {
			RegionImageGenerator.loading.remove(region);
		}
	}

	private static void setSaving(Point2i region, boolean saving) {
		if (saving) {
			RegionImageGenerator.saving.add(region);
		} else {
			RegionImageGenerator.saving.remove(region);
		}
	}

	public static boolean isSaving(Tile tile) {
		return saving.contains(tile.getLocation());
	}

	public static void setOnSaved(Tile tile, Runnable action) {
		onSaved.put(tile.getLocation(), action);
	}

	public static boolean hasActionOnSave(Tile tile) {
		return onSaved.containsKey(tile.getLocation());
	}

	public static class MCAImageLoadJob extends LoadDataJob {

		private final Tile tile;
		private final UUID world;
		private final BiConsumer<Image, UUID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		private MCAImageLoadJob(Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(tile.getLocation(), null, null, null));
			this.tile = tile;
			this.world = world;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			byte[] data = load(tile.getMCAFile());
			if (data != null) {
				MCAFilePipe.executeProcessData(new MCAImageProcessJob(tile.getMCAFile(), data, tile, world, callback, scaleSupplier, scaleOnly, progressChannel));
				return;
			}
			callback.accept(null, world);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		@Override
		public void cancel() {
			setLoading(tile.getLocation(), false);
		}

		public Tile getTile() {
			return tile;
		}
	}

	private static class MCAImageProcessJob extends ProcessDataJob {

		private final File file;
		private final Tile tile;
		private final UUID world;
		private final BiConsumer<Image, UUID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		private MCAImageProcessJob(File file, byte[] data, Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data, null, null);
			this.file = file;
			this.tile = tile;
			this.world = world;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Debug.dumpf("generating image for %s", file.getAbsolutePath());

			Timer t = new Timer();

			File file = tile.getMCAFile();
			ByteArrayReader ptr = new ByteArrayReader(getRegionData());
			RegionMCAFile mcaFile = new RegionMCAFile(file);
			Image image = null;
			try {
				mcaFile.load(ptr);
				Debug.dumpf("took %s to read mca file %s", t, mcaFile.getFile().getName());

				t.reset();

				image = TileImage.generateImage(tile, world, callback, scaleSupplier, mcaFile);

				if (image != null) {
					BufferedImage img = SwingFXUtils.fromFXImage(image, null);
					int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
					BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / zoomLevel);
					Image scaledImage = SwingFXUtils.toFXImage(scaled, null);
					callback.accept(scaledImage, world);
				} else {
					callback.accept(null, world);
				}


			} catch (IOException ex) {
				Debug.errorf("failed to read mca file header from %s", file);
				callback.accept(null, world);
			}


			if (image != null) {
				setSaving(tile.getLocation(), true);
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(image, tile.getLocation(), world, scaleSupplier, scaleOnly, progressChannel));
			} else {
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
			}
		}

		@Override
		public void cancel() {
			setLoading(tile.getLocation(), false);
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private final Point2i region;
		private final UUID world;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		public MCAImageSaveCacheJob(Image data, Point2i region, UUID world, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(region, null, null, null), data);
			this.region = region;
			this.world = world;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			//save image to cache
			try {
				BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
				if (scaleOnly) {
					int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
					File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(world, zoomLevel), region);
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
					}

					BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / (double) zoomLevel);
					Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
					ImageIO.write(scaled, "png", cacheFile);

				} else {
					for (int i = Config.getMinZoomLevel(); i <= Config.getMaxZoomLevel(); i *= 2) {
						File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(world, i), region);
						if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
							Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
						}

						BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / (double) i);
						Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
						ImageIO.write(scaled, "png", cacheFile);
					}
				}
			} catch (IOException ex) {
				Debug.dumpException("failed to save images to cache for " + region, ex);
			}

			setSaving(region, false);

			Runnable r = onSaved.get(region);
			if (r != null) {
				r.run();
				onSaved.remove(region);
			}

			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(region));
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, FileHelper.createMCAFileName(region), FileHelper.createPNGFileName(region));
		}

		@Override
		public void cancel() {
			setSaving(region, false);
		}
	}
}
