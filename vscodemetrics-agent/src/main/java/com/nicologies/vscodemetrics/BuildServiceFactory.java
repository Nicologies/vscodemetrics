package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class BuildServiceFactory implements CommandLineBuildServiceFactory, AgentBuildRunnerInfo {
    private static final Logger LOG = Logger.getLogger(BuildServiceFactory.class);

    private final ArtifactsWatcher myArtifactsWatcher;

    public BuildServiceFactory(@NotNull final ArtifactsWatcher artifactsWatcher) {
        myArtifactsWatcher = artifactsWatcher;
    }

    @NotNull
    public String getType() {
        return CodeMetricConstants.RunnerType;
    }

    public boolean canRun(@NotNull final BuildAgentConfiguration agentConfiguration) {
        if (!agentConfiguration.getSystemInfo().isWindows()) {
            LOG.debug(getType() + " runner is supported only under Windows platform");
            return false;
        }
        return true;
    }


    @NotNull
    public CommandLineBuildService createService() {
        return new BuildService(myArtifactsWatcher);
    }

    @NotNull
    public AgentBuildRunnerInfo getBuildRunnerInfo() {
        return this;
    }
}