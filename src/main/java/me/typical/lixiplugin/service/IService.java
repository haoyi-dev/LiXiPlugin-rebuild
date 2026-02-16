package me.typical.lixiplugin.service;

/** Service interface: setup() on enable, shutdown() on disable. */
public interface IService {
    void setup();
    void shutdown();
}
