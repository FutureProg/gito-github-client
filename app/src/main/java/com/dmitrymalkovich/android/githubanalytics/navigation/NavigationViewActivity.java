package com.dmitrymalkovich.android.githubanalytics.navigation;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dmitrymalkovich.android.githubanalytics.R;
import com.dmitrymalkovich.android.githubanalytics.dashboard.DashboardFragment;
import com.dmitrymalkovich.android.githubanalytics.dashboard.DashboardPresenter;
import com.dmitrymalkovich.android.githubanalytics.data.source.GithubRepository;
import com.dmitrymalkovich.android.githubanalytics.data.source.local.LoaderProvider;
import com.dmitrymalkovich.android.githubanalytics.data.source.local.contract.UserContract;
import com.dmitrymalkovich.android.githubanalytics.data.sync.SyncAdapter;
import com.dmitrymalkovich.android.githubanalytics.notifications.NotificationsService;
import com.dmitrymalkovich.android.githubanalytics.repositories.PublicRepositoryFragment;
import com.dmitrymalkovich.android.githubanalytics.repositories.PublicRepositoryPresenter;
import com.dmitrymalkovich.android.githubanalytics.trending.TrendingRepositoryFragment;
import com.dmitrymalkovich.android.githubanalytics.trending.TrendingRepositoryPresenter;
import com.dmitrymalkovich.android.githubanalytics.util.ActivityUtils;
import com.dmitrymalkovich.android.githubanalytics.welcome.WelcomeActivity;

public class NavigationViewActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor> {
    private static final int USER_LOADER = 101;
    // Delay to launch nav drawer item, to allow close animation to play
    private static final int NAV_VIEW_LAUNCH_DELAY = 250;
    @SuppressWarnings("unused")
    private static final String LOG_TAG = NavigationViewActivity.class.getSimpleName();
    private NavigationView mNavigationView;
    public static final String EXTRA_CURRENT_FRAGMENT = "EXTRA_CURRENT_FRAGMENT";
    private String mCurrentFragment = DashboardFragment.class.getSimpleName();
    private LoaderProvider mLoaderProvider;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_view_open, R.string.navigation_view_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        GithubRepository repository = GithubRepository.Injection.provideGithubRepository(this);
        mNavigationView.getMenu().findItem(R.id.nav_notifications)
                .setIcon(repository.notificationEnabled() ?
                        R.drawable.ic_notifications_active_black_24dp :
                        R.drawable.ic_notifications_off_black_24dp);

        if (Build.VERSION.SDK_INT >= 21) {
            mNavigationView.getMenu().findItem(R.id.nav_notifications)
                    .setVisible(Build.VERSION.SDK_INT >= 21);

            JobScheduler scheduler = (JobScheduler)
                    getSystemService(Context.JOB_SCHEDULER_SERVICE);

            JobInfo.Builder builder = new JobInfo.Builder(1,
                    new ComponentName(this,
                            NotificationsService.class));
            builder.setPeriodic(1000 * 60);
//            builder.setPersisted(true);
//            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            // builder.setRequiresDeviceIdle(true);
            scheduler.cancelAll();
            int result = scheduler.schedule(builder.build());
            if (result == JobScheduler.RESULT_SUCCESS)
                Log.d(LOG_TAG, "Job scheduled successfully!");
        }


        if (savedInstanceState == null) {
            showDashboard();
            SyncAdapter.initializeSyncAdapter(this);
        } else {
            mCurrentFragment = savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT);
            if (mCurrentFragment.equals(DashboardFragment.class.getSimpleName())) {
                showDashboard();
            } else if (mCurrentFragment.equals(PublicRepositoryFragment.class.getSimpleName())) {
                showRepositories();
            } else {
                showTrendingRepositories();
            }
        }

        mLoaderProvider = new LoaderProvider(this);
        getSupportLoaderManager().initLoader(USER_LOADER, null, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CURRENT_FRAGMENT, mCurrentFragment);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final int id = item.getItemId();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (id == R.id.nav_dashboard) {
                    showDashboard();
                } else if (id == R.id.nav_repositories) {
                    showRepositories();
                } else if (id == R.id.nav_trending) {
                    showTrendingRepositories();
                } else if (id == R.id.nav_feedback) {
                    ActivityUtils.openFeedback(NavigationViewActivity.this);
                } else if (id == R.id.nav_sign_out) {
                    signOut();
                } else if (id == R.id.nav_notifications) {
                    GithubRepository repository = GithubRepository.Injection
                            .provideGithubRepository(NavigationViewActivity.this);
                    if (repository.notificationEnabled()) {
                        item.setIcon(R.drawable.ic_notifications_off_black_24dp);
                    } else {
                        item.setIcon(R.drawable.ic_notifications_active_black_24dp);
                    }
                    repository.toggleNotifications();
                }

            }
        }, NAV_VIEW_LAUNCH_DELAY);

        if (id != R.id.nav_notifications) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return mLoaderProvider.createUsersLoader();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            View headerLayout = mNavigationView.getHeaderView(0);
            final ImageView avatarView = (ImageView) headerLayout.findViewById(R.id.avatar);
            final TextView nameView = (TextView) headerLayout.findViewById(R.id.name);
            final TextView followersView = (TextView) headerLayout.findViewById(R.id.followers);

            String avatar = data.getString(UserContract.UsersEntry.COL_AVATAR);
            String name = data.getString(UserContract.UsersEntry.COL_NAME);
            String followers = data.getString(UserContract.UsersEntry.COL_FOLLOWERS);
            Glide.with(getApplicationContext())
                    .load(avatar).into(avatarView);
            nameView.setText(name);
            followersView.setText(getString(R.string.navigation_view_header_followers, followers));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void showDashboard() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentById(R.id.content_navigation_view);
        DashboardFragment dashboardFragment;
        if (fragment instanceof DashboardFragment) {
            dashboardFragment = (DashboardFragment) fragment;
        } else {
            dashboardFragment = DashboardFragment.newInstance();
            ActivityUtils.replaceFragment(getSupportFragmentManager(),
                    dashboardFragment, R.id.content_navigation_view);
        }
        new DashboardPresenter(
                GithubRepository.Injection.provideGithubRepository(this),
                dashboardFragment,
                new LoaderProvider(this),
                getSupportLoaderManager());

        mCurrentFragment = DashboardFragment.class.getSimpleName();
    }

    private void showRepositories() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentById(R.id.content_navigation_view);
        PublicRepositoryFragment publicRepositoriesFragment;
        if (fragment instanceof PublicRepositoryFragment) {
            publicRepositoriesFragment = (PublicRepositoryFragment) fragment;
        } else {
            publicRepositoriesFragment = PublicRepositoryFragment.newInstance();
            ActivityUtils.replaceFragment(getSupportFragmentManager(),
                    publicRepositoriesFragment, R.id.content_navigation_view);
        }
        new PublicRepositoryPresenter(
                GithubRepository.Injection.provideGithubRepository(this),
                publicRepositoriesFragment,
                new LoaderProvider(this),
                getSupportLoaderManager());

        mCurrentFragment = PublicRepositoryFragment.class.getSimpleName();
    }

    private void showTrendingRepositories() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentById(R.id.content_navigation_view);
        TrendingRepositoryFragment trendingRepositoryFragment;
        if (fragment instanceof TrendingRepositoryFragment) {
            trendingRepositoryFragment = (TrendingRepositoryFragment) fragment;
        } else {
            trendingRepositoryFragment = TrendingRepositoryFragment.newInstance();
            ActivityUtils.replaceFragment(getSupportFragmentManager(),
                    trendingRepositoryFragment, R.id.content_navigation_view);
        }
        new TrendingRepositoryPresenter(
                GithubRepository.Injection.provideGithubRepository(this),
                trendingRepositoryFragment,
                new LoaderProvider(this),
                getSupportLoaderManager());

        mCurrentFragment = TrendingRepositoryPresenter.class.getSimpleName();
    }

    private void signOut() {
        GithubRepository.Injection.provideGithubRepository(this).logout();

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
    }
}
