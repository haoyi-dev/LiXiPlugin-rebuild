package me.typical.lixiplugin.service;

/**
 * Base interface for all plugin services.
 * Services are initialized on plugin enable and shut down on plugin disable.
 */
public interface IService {
    /**
     * Called when the service is being initialized.
     * This is where you should set up resources, register listeners, etc.
     */
    void setup();

    /**
     * Called when the service is being shut down.
     * This is where you should clean up resources, close connections, etc.
     */
    void shutdown();
}
