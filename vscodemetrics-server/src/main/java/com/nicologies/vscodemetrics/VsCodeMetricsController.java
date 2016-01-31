package com.nicologies.vscodemetrics;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VsCodeMetricsController extends BaseController {
    private PluginDescriptor myDescriptor;
    public VsCodeMetricsController(WebControllerManager manager, PluginDescriptor descriptor){
        myDescriptor = descriptor;
        manager.registerController("/vscodemetrics.html", this);
        manager.registerController("/savevscodemetrics.html", this);
    }
    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response) throws Exception {
        return new ModelAndView(myDescriptor.getPluginResourcesPath("notReady.jsp"));
    }

    private void SaveMetricsResult(int buildNum){

    }
}
