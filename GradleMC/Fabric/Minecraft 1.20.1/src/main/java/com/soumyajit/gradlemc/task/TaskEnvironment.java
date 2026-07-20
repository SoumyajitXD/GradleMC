package com.soumyajit.gradlemc.task;

/** Thread/environment prerequisite; live Minecraft data must be snapshotted before background work. */
public enum TaskEnvironment { ANY, CLIENT, SERVER, WORLD, PLAYER }
