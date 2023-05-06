package com.example.mobilenet;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class PythonRuntime {

    Python py;
    PyObject pyScript;

    public PythonRuntime(Python instance, String scriptName) {
        this.py = instance;
        this.pyScript = py.getModule(scriptName);
    }

    public PyObject execute() throws Throwable {
        return pyScript.callAttrThrows("main");
    }

    public PyObject execute(String funcName) throws Throwable {
        if (funcName.isEmpty()) {
            funcName = "main";
        }
        return pyScript.callAttrThrows(funcName);
    }

    public PyObject execute(String funcName, Object... args) throws Throwable {
        if (funcName.isEmpty()) {
            funcName = "main";
        }
        return pyScript.callAttrThrows(funcName, args);
    }

    public void executeWithListener(PythonRuntimeListener listener, Object... args) {
        PyObject pyResult = pyScript.callAttr("execute_with_iterator", args);
        PyObject pyOutputIter = pyResult.asList().get(0);
        PyObject pyExecutionFinished = pyResult.asList().get(1);

        // Iterate over the output iterator
        while (!pyExecutionFinished.callAttr("is_set").toBoolean()) {
            PyObject nextOutputResult = pyScript.callAttr("get_next_output_item", pyOutputIter);
            PyObject output = nextOutputResult.asList().get(0);
            boolean hasNext = nextOutputResult.asList().get(1).toBoolean();
            if (hasNext) {
                listener.onIterate(output.toString());
            }
        }

        if (pyExecutionFinished.callAttr("is_set").toBoolean()) {
            listener.onFinish();
        }
    }

    public interface PythonRuntimeListener {
        void onIterate(String value);
        void onFinish();
    }


}
