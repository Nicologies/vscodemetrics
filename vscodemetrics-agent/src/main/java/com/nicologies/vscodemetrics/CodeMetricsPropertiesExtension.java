package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.positioning.PositionAware;
import jetbrains.buildServer.util.positioning.PositionConstraint;
import org.jetbrains.annotations.NotNull;

public class CodeMetricsPropertiesExtension extends AgentLifeCycleAdapter implements PositionAware {

    @NotNull
    private final MetricsExeSearcher mySearcher;

    public CodeMetricsPropertiesExtension(@NotNull final EventDispatcher<AgentLifeCycleListener> events,
                                          @NotNull final MetricsExeSearcher searcher) {
        mySearcher = searcher;
        events.addListener(this);
    }

    @NotNull
    public String getOrderId() {
        return CodeMetricConstants.RunnerType;
    }

    @NotNull
    public PositionConstraint getConstraint() {
        return PositionConstraint.last();
    }

    @Override
    public void agentInitialized(@NotNull final BuildAgent agent) {
        mySearcher.search(agent.getConfiguration());
    }
}
