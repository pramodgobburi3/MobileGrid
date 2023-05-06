import threading
import os
import queue
import sys
import io
import time

class RedirectStdout:
    def __init__(self):
        self._stdout = None
        self._string_io = None

    def __enter__(self):
        self._stdout = sys.stdout
        sys.stdout = self._string_io = io.StringIO()
        return self._string_io

    def __exit__(self, exc_type, exc_val, exc_tb):
        sys.stdout = self._stdout

def execute(task_id, script_path, args=""):
    output_queue = queue.Queue()
    error_queue = queue.Queue()
    task_path = os.path.join(os.environ["HOME"], task_id)
    script_path = os.path.join(task_path, script_path)

    def run_script():
        with open(script_path, 'r') as script_file:
            script_contents = script_file.read()
        with RedirectStdout() as output:
            try:
                script_globals = globals().copy()
                script_globals['__name__'] = '__main__'
                script_globals['sys'] = sys

                # Update sys.argv within the inner script
                original_argv = sys.argv
                if args == "":
                    sys.argv = [script_path, task_path]
                else:
                    sys.argv = [script_path, task_path] + args.split(' ')

                exec(script_contents, script_globals)

                # Restore the original sys.argv
                sys.argv = original_argv
            except Exception as e:
                error_queue.put(e)
        output_queue.put(output.getvalue())

    script_thread = threading.Thread(target=run_script)
    script_thread.start()
    script_thread.join()

    if not error_queue.empty():
        raise error_queue.get()

    return output_queue.get()

def execute_with_iterator(task_id, script_path, args=""):
    output_queue = queue.Queue()
    error_queue = queue.Queue()
    task_path = os.path.join(os.environ["HOME"], task_id)
    script_path = os.path.join(task_path, script_path)
    execution_finished = threading.Event()

    def run_script():
        with open(script_path, 'r') as script_file:
            script_contents = script_file.read()
        with RedirectStdout() as output:
            try:
                script_globals = globals().copy()
                script_globals['__name__'] = '__main__'
                script_globals['sys'] = sys

                # Update sys.argv within the inner script
                original_argv = sys.argv
                if args == "":
                    sys.argv = [script_path, task_path]
                else:
                    sys.argv = [script_path, task_path] + args.split(' ')

                exec(script_contents, script_globals)

                # Restore the original sys.argv
                sys.argv = original_argv
            except Exception as e:
                error_queue.put(e)
        output_queue.put(output.getvalue())
        execution_finished.set()

    def output_iterator():
        script_thread = threading.Thread(target=run_script)
        script_thread.start()

        while script_thread.is_alive():
            time.sleep(1)
            if not output_queue.empty():
                yield output_queue.get()

        script_thread.join()

        if not error_queue.empty():
            raise error_queue.get()

        if not output_queue.empty():
            yield output_queue.get()

    return output_iterator(), execution_finished

def get_next_output_item(output_iter):
    try:
        return next(output_iter), True
    except StopIteration:
        return None, False
