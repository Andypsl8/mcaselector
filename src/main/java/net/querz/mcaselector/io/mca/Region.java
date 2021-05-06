package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.io.ByteArrayReader;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.range.Range;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// holds data for chunks, poi and entities
public class Region {

	private RegionMCAFile region;
	private PoiMCAFile poi;
	private EntitiesMCAFile entities;

	private RegionDirectories directories;

	private Point2i location;

	public static Region loadRegion(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData) throws IOException {
		Region r = new Region();
		if (dirs.getRegion() != null && regionData != null) {
			r.loadRegion(dirs.getRegion(), new ByteArrayReader(regionData));
			r.location = dirs.getLocation();
		}
		if (dirs.getPoi() != null && poiData != null) {
			r.loadPoi(dirs.getPoi(), new ByteArrayReader(poiData));
		}
		if (dirs.getEntities() != null && entitiesData != null) {
			r.loadEntities(dirs.getEntities(), new ByteArrayReader(entitiesData));
		}
		r.directories = dirs;
		return r;
	}

	public static Region loadRegion(RegionDirectories dirs) throws IOException {
		Region r = new Region();
		if (dirs.getRegion() != null) {
			r.loadRegion(dirs.getRegion());
		}
		if (dirs.getPoi() != null) {
			r.loadPoi(dirs.getPoi());
		}
		if (dirs.getEntities() != null) {
			r.loadEntities(dirs.getEntities());
		}
		r.directories = dirs;
		return r;
	}

	public static Region loadRegionHeaders(RegionDirectories dirs, byte[] regionHeader, byte[] poiHeader, byte[] entitiesHeader) throws IOException {
		Region r = new Region();
		if (dirs.getRegion() != null && regionHeader != null) {
			r.region = new RegionMCAFile(dirs.getRegion());
			r.region.loadHeader(new ByteArrayReader(regionHeader));
		}
		if (dirs.getPoi() != null && poiHeader != null) {
			r.poi = new PoiMCAFile(dirs.getPoi());
			r.poi.loadHeader(new ByteArrayReader(poiHeader));
		}
		if (dirs.getEntities() != null && entitiesHeader != null) {
			r.entities = new EntitiesMCAFile(dirs.getEntities());
			r.entities.loadHeader(new ByteArrayReader(entitiesHeader));
		}
		r.directories = dirs;
		return r;
	}

	public static Region loadOrCreateEmptyRegion(RegionDirectories dirs) throws IOException {
		Region r = new Region();
		if (dirs.getRegion() != null) {
			if (dirs.getRegion().exists()) {
				r.loadRegion(dirs.getRegion());
			} else {
				r.region = new RegionMCAFile(dirs.getRegion());
			}
		}
		if (dirs.getPoi() != null) {
			if (dirs.getPoi().exists()) {
				r.loadPoi(dirs.getPoi());
			} else {
				r.poi = new PoiMCAFile(dirs.getPoi());
			}
		}
		if (dirs.getEntities() != null) {
			if (dirs.getEntities().exists()) {
				r.loadEntities(dirs.getEntities());
			} else {
				r.entities = new EntitiesMCAFile(dirs.getEntities());
			}
		}
		return r;
	}

	public void loadRegion(File src) throws IOException {
		region = new RegionMCAFile(src);
		region.load();
	}

	public void loadRegion(File src, ByteArrayReader ptr) throws IOException {
		region = new RegionMCAFile(src);
		region.load(ptr);
	}

	public void loadPoi(File src) throws IOException {
		poi = new PoiMCAFile(src);
		poi.load();
	}

	public void loadPoi(File src, ByteArrayReader ptr) throws IOException {
		poi = new PoiMCAFile(src);
		poi.load(ptr);
	}

	public void loadEntities(File src) throws IOException {
		entities = new EntitiesMCAFile(src);
		entities.load();
	}

	public void loadEntities(File src, ByteArrayReader ptr) throws IOException {
		entities = new EntitiesMCAFile(src);
		entities.load(ptr);
	}

	public RegionMCAFile getRegion() {
		return region;
	}

	public PoiMCAFile getPoi() {
		return poi;
	}

	public EntitiesMCAFile getEntities() {
		return entities;
	}

	public void setRegion(RegionMCAFile region) {
		this.region = region;
	}

	public void setPoi(PoiMCAFile poi) {
		this.poi = poi;
	}

	public void setEntities(EntitiesMCAFile entities) {
		this.entities = entities;
	}

	public boolean isEmpty() {
		boolean empty = true;
		if (region != null) {
			empty = region.isEmpty();
		}
		if (poi != null && empty) {
			empty = poi.isEmpty();
		}
		if (entities != null && empty) {
			empty = entities.isEmpty();
		}
		return empty;
	}

	public void setDirectories(RegionDirectories dirs) {
		if (region != null) {
			region.setFile(dirs.getRegion());
		}
		if (poi != null) {
			poi.setFile(dirs.getPoi());
		}
		if (entities != null) {
			entities.setFile(dirs.getEntities());
		}
	}

	public ChunkData getChunkDataAt(Point2i location) {
		RegionChunk regionChunk = null;
		PoiChunk poiChunk = null;
		EntitiesChunk entitiesChunk = null;
		if (region != null) {
			regionChunk = region.getChunkAt(location);
		}
		if (poi != null) {
			poiChunk = poi.getChunkAt(location);
		}
		if (entities != null) {
			entitiesChunk = entities.getChunkAt(location);
		}
		return new ChunkData(regionChunk, poiChunk, entitiesChunk);
	}

	public ChunkData getChunkData(int index) {
		RegionChunk regionChunk = null;
		PoiChunk poiChunk = null;
		EntitiesChunk entitiesChunk = null;
		if (region != null) {
			regionChunk = region.getChunk(index);
		}
		if (poi != null) {
			poiChunk = poi.getChunk(index);
		}
		if (entities != null) {
			entitiesChunk = entities.getChunk(index);
		}
		return new ChunkData(regionChunk, poiChunk, entitiesChunk);
	}

	public void setChunkDataAt(ChunkData chunkData, Point2i location) {
		if (region == null && directories.getRegion() != null) {
			region = new RegionMCAFile(directories.getRegion());
		}
		if (poi == null && directories.getPoi() != null) {
			poi = new PoiMCAFile(directories.getPoi());
		}
		if (entities == null && directories.getEntities() != null) {
			entities = new EntitiesMCAFile(directories.getEntities());
		}
		if (region != null) {
			region.setChunkAt(location, chunkData.getRegion());
		}
		if (poi != null) {
			poi.setChunkAt(location, chunkData.getPoi());
		}
		if (entities != null) {
			entities.setChunkAt(location, chunkData.getEntities());
		}
	}

	public void save() throws IOException {
		if (region != null) {
			region.save();
		}
		if (poi != null) {
			poi.save();
		}
		if (entities != null) {
			entities.save();
		}
	}

	public void saveWithTempFiles() throws IOException {
		if (region != null) {
			region.saveWithTempFile();
		}
		if (poi != null) {
			poi.saveWithTempFile();
		}
		if (entities != null) {
			entities.saveWithTempFile();
		}
	}

	public void saveWithTempFiles(RegionDirectories dest) throws IOException {
		if (region != null) {
			region.saveWithTempFile(dest.getRegion());
		}
		if (poi != null) {
			poi.saveWithTempFile(dest.getPoi());
		}
		if (entities != null) {
			entities.saveWithTempFile(dest.getEntities());
		}
	}

	public void deFragment() throws IOException {
		if (region != null) {
			region.deFragment();
		}
		if (poi != null) {
			poi.deFragment();
		}
		if (entities != null) {
			entities.deFragment();
		}
	}

	public void deFragment(RegionDirectories dest) throws IOException {
		if (region != null) {
			region.deFragment(dest.getRegion());
		}
		if (poi != null) {
			poi.deFragment(dest.getPoi());
		}
		if (entities != null) {
			entities.deFragment(dest.getEntities());
		}
	}

	public void deleteFiles() {
		if (directories.getRegion() != null && directories.getRegion().exists()) {
			directories.getRegion().delete();
		}
		if (directories.getPoi() != null && directories.getPoi().exists()) {
			directories.getPoi().delete();
		}
		if (directories.getEntities() != null && directories.getEntities().exists()) {
			directories.getEntities().delete();
		}
	}

	public void deleteChunks(Set<Point2i> selection) {
		if (region != null) {
			region.deleteChunks(selection);
		}
		if (poi != null) {
			poi.deleteChunks(selection);
		}
		if (entities != null) {
			entities.deleteChunks(selection);
		}
	}

	public boolean deleteChunks(Filter<?> filter, Set<Point2i> selection) {
		boolean deleted = false;
		for (int i = 0; i < 1024; i++) {
			RegionChunk region = this.region.getChunk(i);
			EntitiesChunk entities = this.entities == null ? null : this.entities.getChunk(i);
			PoiChunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty() || selection != null && !selection.contains(region.getAbsoluteLocation())) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, poi, entities);

			if (filter.matches(filterData)) {
				deleteChunkIndex(i);
				deleted = true;
			}
		}
		return deleted;
	}

	public boolean keepChunks(Filter<?> filter, Set<Point2i> selection) {
		boolean deleted = false;
		for (int i = 0; i < 1024; i++) {
			RegionChunk region = this.region.getChunk(i);
			EntitiesChunk entities = this.entities == null ? null : this.entities.getChunk(i);
			PoiChunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty()) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, poi, entities);

			// keep chunk if filter AND selection applies
			// ignore selection if it's null
			if (!filter.matches(filterData) || selection != null && !selection.contains(region.getAbsoluteLocation())) {
				deleteChunkIndex(i);
				deleted = true;
			}
		}
		return deleted;
	}

	private void deleteChunkIndex(int index) {
		if (this.region != null) {
			this.region.deleteChunk(index);
		}
		if (this.entities != null) {
			this.entities.deleteChunk(index);
		}
		if (this.poi != null) {
			this.poi.deleteChunk(index);
		}
	}

	public Set<Point2i> getFilteredChunks(Filter<?> filter, Set<Point2i> selection) {
		Set<Point2i> chunks = new HashSet<>();

		for (int i = 0; i < 1024; i++) {
			RegionChunk region = this.region.getChunk(i);
			EntitiesChunk entities = this.entities == null ? null : this.entities.getChunk(i);
			PoiChunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty()) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, poi, entities);

			Point2i location = region.getAbsoluteLocation();
			try {
				if (filter.matches(filterData)) {
					if (location == null) {
						continue;
					}
					if (selection == null || selection.contains(location)) {
						chunks.add(location);
					}
				}
			} catch (Exception ex) {
				Debug.dumpException(String.format("failed to select chunk %s", location), ex);
			}
		}
		return chunks;
	}

	public void applyFieldChanges(List<Field<?>> fields, boolean force, Set<Point2i> selection) {
		Timer t = new Timer();
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				Point2i absoluteLocation = location.regionToChunk().add(x, z);
				ChunkData chunkData = getChunkDataAt(absoluteLocation);
				if (selection == null || selection.contains(absoluteLocation)) {
					try {
						chunkData.applyFieldChanges(fields, force);
					} catch (Exception ex) {
						Debug.dumpException("failed to apply field changes to chunk " + absoluteLocation, ex);
					}
				}
			}
		}
		Debug.printf("took %s to apply field changes to region %s", t, location);
	}

	public void mergeInto(Region region, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		if (this.region != null) {
			this.region.mergeChunksInto(region.region, offset, overwrite, sourceChunks, selection, ranges);
		}
		if (this.poi != null) {
			this.poi.mergeChunksInto(region.poi, offset, overwrite, sourceChunks, selection, ranges);
		}
		if (this.entities != null) {
			this.entities.mergeChunksInto(region.entities, offset, overwrite, sourceChunks, selection, ranges);
		}
	}
}
