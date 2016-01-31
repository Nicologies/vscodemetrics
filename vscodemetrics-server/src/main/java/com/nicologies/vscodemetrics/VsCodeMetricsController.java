package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;

public class VsCodeMetricsController extends BaseController {
    private static final Logger LOG = jetbrains.buildServer.log.Loggers.SERVER;
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
        HashMap<String, Object> model = new HashMap<String, Object>();
        String view = "";
        if(request.getRequestURI().equals("/savevscodemetrics.html")) {
            String repo = request.getParameter("repo");
            String build = request.getParameter("build");
            model.put("repo", repo);
            model.put("build", build);
            view = myDescriptor.getPluginResourcesPath("savevscodemetrics.jsp");
        }

        return new ModelAndView(view, model);
    }

    private void SaveMetricsResult(int buildNum){

    }
}
