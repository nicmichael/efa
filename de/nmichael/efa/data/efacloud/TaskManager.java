/*
 * <pre>
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael, Martin Glade
 * @version 2</pre>
 */
package de.nmichael.efa.data.efacloud;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * <p>A utility class to manage all background tasks. </p> <p>Expected implementation:</p> <ol>
 * <li>Run the UI on a single Thread in the main activity</li> <li>implement the RequestHandlerIF
 * in
 * this main activity and add a UIaccessHandler from the JackA or JackJ class to it.</li>
 * <li>Implement all application logic in engines and add a TaskManager to each.</li> <li>Implement
 * the handleRequest() methods within the engines. For long running tasks make sure the engine polls
 * the RunControl element of the running request and stops execution, if so requested.</li> </ol>
 * <p>You can then dispatch workload by sending messages to the engines' task handlers. Set the
 * UIaccessHandler to be the callback object of the requested task. Results and progress information
 * shall be fed back autonomously from the engine to the call back object during task
 * execution.</p>
 * <p>Created by mgSoft on 05.12.16.</p>
 */
@SuppressWarnings("unused")
public class TaskManager {


    public static final int UI_PROGRESS_FINITE = 10;
    public static final int UI_PROGRESS_INFINITE = 20;
    public static final int UI_SHOW_INFO_DIALOG = 30;
    public static final int UI_SHOW_TOAST = 40;


    private final Vector<RequestMessage> taskQueue = new Vector<RequestMessage>();
    private int messagesHandled;
    private RequestMessage activeTask = null;
    private Timer timer;

    /**
     * Construct the task manager for control message management.
     *
     * @param requestHandler  the object to act on the messages. Although it implements the
     *                        RequestDispatcherIF, it is not expected to queue any request, but
     *                        rather act on them. The task manager will run and manage the queue.
     * @param timerInterval   the frequency with which the task manager shall check the queue
     * @param timerStartDelay the delay after which to start polling the queue, starting with this
     *                        construction call.
     */
    public TaskManager(final RequestHandlerIF requestHandler,
                       final int timerInterval, final int timerStartDelay) {
        // no lambda, keep it Java 7 compatible.
        TimerTask timerTask = new TimerTask() {
            public void run() {
                if ((activeTask == null) && !taskQueue.isEmpty()) {
                    final RequestMessage taskMsg = taskQueue.firstElement();
                    taskQueue.remove(0);
                    messagesHandled++;
                    activeTask = taskMsg;
                    // no lambda, keep it Java 7 compatible.
                    Thread timerTaskThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            requestHandler.handleRequest(taskMsg);
                            activeTask = null;
                        }
                    });
                    timerTaskThread.start();
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, timerStartDelay, timerInterval);
    }

    /**
     * Terminate task manager by canceling timer execution. Call this method prior to dropping the
     * task manager, e. g. by re-instantiation.
     */
    public void cancel() {
        timer.cancel();
        timer = null;
        clearQueue();
    }

    /**
     * Appends a tasks to the queue.
     *
     * @param request request for task which is to be added
     */
    public void addTask(RequestMessage request) {
        taskQueue.add(request);
    }

    /**
     * @return count of requests till pending for execution.
     */
    public int requestsPending() {
        return taskQueue.size();
    }

    /**
     * Stops the active task (if there is one running) and clears all pending tasks from the task
     * queue. The timer is not cancelled, so that you can addd new tasks to the queue and they will
     * be executed.
     */
    public void clearQueue() {
        if (activeTask != null) {
            activeTask.runControl.stop();
            activeTask = null;
        }
        taskQueue.removeAllElements();
    }

    @Override
    public String toString() {
        return "TaskManager@" + hashCode() + " -open " + taskQueue.size() + "/" + messagesHandled;
    }

    /**
     * Interface to take messages into the queue.
     */
    public interface RequestDispatcherIF {

        /**
         * <p>Take a message into the message queue. The implementation must not act on the request
         * in this method, but rather in the handleRequest() method. This shall be used both ways:
         * UI to background task and background task to UI.</p>
         *
         * @param request request sent
         */
        void sendRequest(RequestMessage request);

    }

    /**
     * Interface to handle messages in the queue.
     */
    public interface RequestHandlerIF {

        /**
         * <p>Act on the given request. The implementation shall immediately execute in the current
         * thread on this request. It shall only be called by the message handler.</p>
         *
         * @param msg request to be handled.
         */
        void handleRequest(RequestMessage msg);
    }

    /**
     * A little class holding just one volatile boolean field to stop AsyncTasks, if needed. The
     * executor of the task related to a specific message shall regularly call keepGoing and stop
     * execution once it is false. This will happen after the stop() Method was called.
     *
     * @author mgSoft
     */
    public static class RunControl {
        private volatile boolean keepGoing = true;

        public boolean keepGoing() {
            return keepGoing;
        }

        public void stop() {
            this.keepGoing = false;
        }
    }

    /**
     * RequestMessage provides a wrapper for pending requests from UI to engine and the other way
     * around. The following fields exist: <ul> <li>title, text for dialogs</li> <li>type and value
     * for tasks to be executed and progress display.</li><li>sender to provide an object for
     * callbak, must implement the TaskManager.RequestDispatcherIF iunterface</li><li>runControl to
     * provide a means to stop execution of a task. The executor shall regularly check the keepGoing
     * flag of the RunControl Object while the requester shall stop it, if the execution result is
     * no more needed.
     * </li></ul>If you need to pass more information to the engine, it is recommended to use the
     * configuration framework provided by Jack with CfgHandle and CfgItem.
     *
     * @author mgSoft
     */
    public static class RequestMessage {
        public final String title, text;
        public final int type;
        public final double value;
        public final Object sender;
        public final RunControl runControl;
        public final long created;
        public long started;
        public long completed;

        /**
         * Constructor. Interpretation of the "values to be used by the invoked method" please
         * consult the respective Handler class descriptions.
         *
         * @param title  for request to UI: title of the UI element. For request to Engine: value to
         *               be used by the invoked method.
         * @param text   for request to UI: text to be shown in the UI element. For request to
         *               Engine: value to be used by the invoked method.
         * @param type   type of message.
         * @param value  value to be used by the invoked method.
         * @param sender the message sender. For request to Engine: callback handler.
         */
        public RequestMessage(String title, String text, int type, double value, Object sender) {
            this.text = text;
            this.title = title;
            this.type = type;
            this.value = value;
            this.sender = sender;
            this.runControl = new RunControl();
            this.created = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "RequestMessage: " + title + ". Sender: " + ((sender == null) ? "null" : sender
                    .toString());
        }
    }

}
