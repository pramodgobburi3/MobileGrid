package com.example.mobilenet.models;

import com.example.mobilenet.PythonRuntime;

public class RuntimeTask {

    private PythonRuntime py;
    private String funcName;
    private Object args;


    public RuntimeTask(PythonRuntime py, String funcName, Object... args) {
        this.py = py;
        this.funcName = funcName;
        this.args = args;
    }

    public PythonRuntime getPy() {
        return py;
    }

    public void setPy(PythonRuntime py) {
        this.py = py;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public Object getArgs() {
        return args;
    }

    public void setArgs(Object... args) {
        this.args = args;
    }
}
