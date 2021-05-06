package net.querz.mcaselector;

import net.querz.mcaselector.headless.HeadlessHelper;
import net.querz.mcaselector.headless.ParamExecutor;
import net.querz.mcaselector.io.net.client.Client;
import net.querz.mcaselector.io.net.client.message.RegionImageMessage;
import net.querz.mcaselector.io.net.event.MessageEventHandler;
import net.querz.mcaselector.io.net.server.Server;
import net.querz.mcaselector.io.net.server.message.WindowInteractMessage;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.validation.ShutdownHooks;
import javax.swing.*;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

	public static Client client;

	public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
		Debug.dumpf("java version: %s", System.getProperty("java.version"));
		Debug.dumpf("jvm max mem:  %d", Runtime.getRuntime().maxMemory());


		MessageEventHandler handler = new MessageEventHandler();
		handler.registerEvent(new WindowInteractMessage());

		Server server = new Server("localhost", 1234, handler);
		server.start();

		MessageEventHandler clientHandler = new MessageEventHandler();

		client = new Client("localhost", 1234, clientHandler);
		client.start();

		ShutdownHooks.addShutdownHook(() -> {
			try {
				client.stop();
				server.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		Future<Boolean> headless = new ParamExecutor(args).run();
		if (headless != null && headless.get()) {
			// we already ran headless mode, so we exit here
			Debug.print("exiting");
			System.exit(0);
		}

		if (!HeadlessHelper.hasJavaFX()) {
			JOptionPane.showMessageDialog(null, "Please install JavaFX for your Java version (" + System.getProperty("java.version") + ") to use MCA Selector.", "Missing JavaFX", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		Config.loadFromIni();
		ShutdownHooks.addShutdownHook(Config::exportConfig);
		if (Config.debug()) {
			Debug.initLogWriter();
		}
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());
		Window.launch(Window.class, args);
	}
}
