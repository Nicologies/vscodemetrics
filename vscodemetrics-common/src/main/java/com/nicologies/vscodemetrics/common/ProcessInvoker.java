package com.nicologies.vscodemetrics.common;

import java.io.InputStream;
import java.util.Scanner;

public class ProcessInvoker {
    private ProcessBuilder pb;

    public ProcessInvoker(ProcessBuilder pb) {
        this.pb = pb;
    }

    public int invoke(){
        try {
            Process process = pb.start();
            redirectStreamToLogger(process.getInputStream(), new RedirectionTarget() {
                public void redirect(String s) {
                    _stdOut += s;
                }
            });
            redirectStreamToLogger(process.getErrorStream(), new RedirectionTarget() {
                public void redirect(String s) {
                    _stdErr += s;
                }
            });

            return process.waitFor();
        }catch(Exception ex){
            _stdErr = ex.toString();
            return -1;
        }
    }

    private interface RedirectionTarget{
        void redirect(String s);
    }

    private String _stdOut = "";
    public String stdOut(){
        return _stdOut;
    }

    private String _stdErr = "";
    public String stdErr(){
        return _stdErr;
    }

    private void redirectStreamToLogger(final InputStream s, final RedirectionTarget target) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(s);
                while (sc.hasNextLine()) {
                    target.redirect(sc.nextLine());
                }
            }
        }).start();
    }
}
