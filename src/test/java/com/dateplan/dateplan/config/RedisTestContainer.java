package com.dateplan.dateplan.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainer {

	private static final GenericContainer<?> CONTAINER;

	static {
		CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.0.5"))
			.withExposedPorts(6379)
			.waitingFor(Wait.forListeningPort())
			.withReuse(false);

		CONTAINER.start();
	}

	public static String getHost() {
		return CONTAINER.getHost();
	}

	public static Integer getPort() {
		return CONTAINER.getMappedPort(6379);
	}

	public static void stop() {
		CONTAINER.stop();
	}
}
