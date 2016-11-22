package com.dmitrymalkovich.android.githubanalytics.notifications;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.dmitrymalkovich.android.githubanalytics.GithubAnalyticsApplication;

/**
 * http://www.vogella.com/tutorials/AndroidTaskScheduling/article.html
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NotificationsService extends JobService {

    private static final String LOG_TAG = NotificationsService.class.getSimpleName();
    private UpdateAppsAsyncTask updateTask = new UpdateAppsAsyncTask();

    @Override
    public boolean onStartJob(JobParameters params) {
        // Note: this is preformed on the main thread.

        Log.d(LOG_TAG, "Job scheduled: onStartJob");
        
        updateTask.execute(params);

        return true;
    }

    // Stopping jobs if our job requires change.

    @Override
    public boolean onStopJob(JobParameters params) {
        // Note: return true to reschedule this job.
        boolean shouldReschedule = updateTask.stopJob(params);

        return shouldReschedule;
    }

    private class UpdateAppsAsyncTask extends AsyncTask<JobParameters, Void, JobParameters[]> {


        @Override
        protected JobParameters[] doInBackground(JobParameters... params) {

            NotificationsManager.getInstance().sendNotification(GithubAnalyticsApplication.context());
            // Do updating and stopping logical here.
            return params;
        }

        @Override
        protected void onPostExecute(JobParameters[] result) {
            for (JobParameters params : result) {
                if (!hasJobBeenStopped(params)) {
                    jobFinished(params, false);
                }
            }
        }

        private boolean hasJobBeenStopped(JobParameters params) {
            // Logic for checking stop.
            return false;
        }

        public boolean stopJob(JobParameters params) {
            // Logic for stopping a job. return true if job should be rescheduled.
            return false;
        }

    }
}

