package com.databazoo.tools;

import com.databazoo.components.UIConstants;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Timed and threaded tasks factory.
 *
 * Examples:
 *
 * <pre>{@code
 *     Schedule.delay(Schedule.CLICK_DELAY, new Runnable() {
 *         @Override
 *         public void run() {
 *             ...
 *         }
 *     });
 * }</pre>
 *
 * <pre>{@code
 *     Schedule.reInvokeInWorker(Schedule.Named.GC, 1000, new Runnable() {
 *         @Override
 *         public void run() {
 *             ...
 *         }
 *     });
 * }</pre>
 *
 * <pre>{@code
 *     Schedule.inWorker(new Runnable() {
 *         @Override
 *         public void run() {
 *             ...
 *         }
 *     });
 * }</pre>
 */
public interface Schedule {

    /**
     * This is a thread pool as created by {@link Executors#newCachedThreadPool}, but with custom keep-alive time to cover for standard DB sync
     * interval.
     */
    Executor THREAD_POOL = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 301L, TimeUnit.SECONDS, new SynchronousQueue<>());

    /**
     * Short delay.
     * Delays task execution until user stops manipulating the UI (dragging operations, etc.)
     *
     * For use with reInvoke*() methods.
     */
    int CLICK_DELAY = 100;

    /**
     * Type delay.
     * Delays task execution until user stops typing into text inputs.
     *
     * For use with reInvoke*() methods.
     */
    int TYPE_DELAY = UIConstants.TYPE_TIMEOUT;

    /**
     * Named timer constants.
     */
    enum Named {
        CANVAS_CHECK_WHITESPACE,
        CANVAS_DRAW_PROJECT,
        CONNECTION_STATUS_CHECK,
        DATA_WINDOW_QUERY_SIZE,
        DBG_BACKUP_ERROR_LOG,
        DIFFERENCE_VIEW_REVISION_SEARCH,
        EXPORT_IMPORT_WIZARD_DB_COMBO_LISTENER,
        GC,
        INTRO_WIZARD_PASSWORDS_MATCH,
        INTRO_WIZARD_START,
        LICENSE_WIZARD_CHECK_LICENSE,
        MIG_WIZARD_INPUT_NOTIFICATION,
        MIG_WIZARD_PATH_LISTENER,
        NAVIGATOR_CHECK_SCHEMATA,
        NEW_VERSION_CHECK,
        PROJECT_SAVE,
        PROJECT_MANAGER_SAVE_PROJECTS,
        PROJECT_SYNC_LOCK_RELEASE,
        PROJECT_WIZARD_INPUT_LISTENER,
        PROJECT_WIZARD_CONN_SELECTION_LISTENER,
        RELATION_DATA_REPAINT,
        RELATION_INFO,
        RELATION_WIZARD_INPUT_LISTENER,
        SEARCH_PANEL_TRIGGER_SEARCH,
        SERVER_ACTIVITY_MERGE_ROWS,
        SERVER_ADMIN_TABLE_EDIT,
        SETTINGS_SAVE,
        TEXT_FIELD_KEY_LISTENER,
        USAGE_LOG_REPEATED,
        HOT_MENU_RECHECK;

        private Timer timeout;
    }

    /**
     * Call given Runnable in a worker thread after a given delay.
     *
     * Calling this method again (with same params) resets the timer.
     *
     * @param named timer constant
     * @param delay in milliseconds
     * @param runnable task to execute
     */
    static void reInvokeInWorker(final Named named, int delay, final Runnable runnable) {
        synchronized (named) {
            if (named.timeout != null) {
                named.timeout.stop();
            }
            named.timeout = new Timer(delay, e -> {
                synchronized (named) {
                    named.timeout.stop();
                    named.timeout = null;
                }
                inWorker(runnable);
            });
            named.timeout.start();
        }
    }

    /**
     * Call given Runnable in Event Dispatch Thread after a given delay.
     *
     * Calling this method again (with same params) resets the timer.
     *
     * @param named timer constant
     * @param delay in milliseconds
     * @param runnable task to execute
     */
    static void reInvokeInEDT(final Named named, int delay, final Runnable runnable) {
        synchronized (named) {
            if (named.timeout != null) {
                named.timeout.stop();
            }
            named.timeout = new Timer(delay, e -> {
                synchronized (named) {
                    named.timeout.stop();
                    named.timeout = null;
                }
                runnable.run();
            });
            named.timeout.start();
        }
    }

    /**
     * Stop named timer.
     *
     * @param named timer constant
     */
    static void stopScheduled(final Named named) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (named) {
            if (named.timeout != null) {
                named.timeout.stop();
            }
        }
    }

    /**
     * Call given Runnable in a worker thread with no delay.
     *
     * @param runnable task to execute
     */
    static void inWorker(final Runnable runnable) {
        THREAD_POOL.execute(runnable);
    }

    /**
     * Call given Runnable in a worker thread after a given delay.
     *
     * @param delay in milliseconds
     * @param runnable task to execute
     */
    static void inWorker(int delay, final Runnable runnable) {
        final Timer timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            timer.stop();
            inWorker(runnable);
        });
        timer.start();
    }

    /**
     * Call given Runnable in Event Dispatch Thread with no delay.
     *
     * @param runnable task to execute
     */
    static void inEDT(final Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Call given Runnable in Event Dispatch Thread after a given delay.
     *
     * @param delay in milliseconds
     * @param runnable task to execute
     */
    static void inEDT(int delay, final Runnable runnable) {
        final Timer timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            timer.stop();
            runnable.run();
        });
        timer.start();
    }

    /**
     * Call given Runnable in Event Dispatch Thread and wait.
     *
     * @param runnable task to execute
     */
    static void waitInEDT(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait(runnable);
        } else {
            runnable.run();
        }
    }
}
