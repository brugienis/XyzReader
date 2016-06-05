package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.ProgressBarHandler;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 *
 * Code handling transitions is based on (Alex Lockwood)
 *     http://stackoverflow.com/questions/27304834/viewpager-fragments-shared-element-transitions and
 *     repo https://github.com/alexjlockwood/activity-transitions.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STAGGERED_GRIDLAYOUT_MANAGER = "staggered_gridlayout_manager";
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private View mCoordinatorlayout;
    private boolean mIsDetailsActivityStarted;
    public static String[] TRANSITION_NAMES;
    private Bundle mTmpReenterState;
    private boolean mIsRefreshing = false;
    private StaggeredGridLayoutManager mStaggeredGridLayoutManager;
    private ProgressBarHandler mProgressBarHandler;

    public static final String LIST_SELECTED_ARTICLE_POSITION = "com.example.xyzreader.ui.LIST_SELECTED_ARTICLE_POSITION";

    private static final String TAG = ArticleListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate - start");
        setContentView(R.layout.activity_article_list);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final SharedElementCallback mCallback = new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mTmpReenterState != null) {
                        int originalCurrentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_ORIGINAL_CURRENT_POSITION);
                        int currentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_THIS_CURRENT_POSITION);
                        if (originalCurrentPosition != currentPosition) {
                            // If startingPosition != currentPosition the user must have swiped to a
                            // different page in the DetailsActivity. We must update the shared element
                            // so that the correct one falls into place.
                            String newTransitionName = TRANSITION_NAMES[currentPosition];
                            View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                            if (newSharedElement != null) {
                                names.clear();
                                names.add(newTransitionName);
                                sharedElements.clear();
                                sharedElements.put(newTransitionName, newSharedElement);
                            }
                        }

                        mTmpReenterState = null;
                    } else {
                        // If mTmpReenterState is null, then the activity is exiting.
                        View navigationBar = findViewById(android.R.id.navigationBarBackground);
                        View statusBar = findViewById(android.R.id.statusBarBackground);
                        if (navigationBar != null) {
                            names.add(navigationBar.getTransitionName());
                            sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                        }
                        if (statusBar != null) {
                            names.add(statusBar.getTransitionName());
                            sharedElements.put(statusBar.getTransitionName(), statusBar);
                        }
                    }
                }
            };
            setExitSharedElementCallback(mCallback);
        }

        mCoordinatorlayout = findViewById(R.id.articleListActivity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                     @Override
                                                     public void onRefresh() {
                                                         refresh();
                                                     }
                                                 }
        );

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        mProgressBarHandler = new ProgressBarHandler(this);
        Log.v(TAG, "onCreate - end");
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mStaggeredGridLayoutManager != null) {
            Log.v(TAG, "onSaveInstanceState - state: " + mStaggeredGridLayoutManager.onSaveInstanceState());
            outState.putParcelable(STAGGERED_GRIDLAYOUT_MANAGER, mStaggeredGridLayoutManager.onSaveInstanceState());
        } else {
            Log.v(TAG, "onSaveInstanceState - mStaggeredGridLayoutManager is null");
        }
//        Log.v(TAG, "onSaveInstanceState - outState: " + outState);
    }

    /**
     * Retrieves saved data.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        Log.v(TAG, "onRestoreInstanceState - savedInstanceState: " + savedInstanceState);

        // FIXME: 3/06/2016 still getting java.lang.NullPointerException: Attempt to invoke virtual method 'void android.support.v7.widget.StaggeredGridLayoutManager.onRestoreInstanceState(android.os.Parcelable)' on a null object reference
        // when in landscape, changing selected article, clicking back button and immediately rotating device to portrait

        // MORE TESTING WITH rotation

        Parcelable state = savedInstanceState.getParcelable(STAGGERED_GRIDLAYOUT_MANAGER);
        Log.v(TAG, "onRestoreInstanceState - state: " + state);
        if (state != null & mStaggeredGridLayoutManager != null) {
//            mStaggeredGridLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(STAGGERED_GRIDLAYOUT_MANAGER));
            mStaggeredGridLayoutManager.onRestoreInstanceState(state);
//            AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.main_appbar);
//            appBarLayout.setExpanded(false);
        }
    }

    /**
     * Start new search.
     */
    private void refresh() {
        if (!mIsRefreshing) {
            startService(new Intent(this, UpdaterService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mProgressBarHandler.hide();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    // FIXME: 5/06/2016 - the bolow seemed to fix the problem, but then I romoved to call
    // to requestLayout() and it is still working
    /**
     *
     * Called when shared elements transition returns back.
     *
     * Had to add mRecyclerView.requestLayout() after the call to
     * mRecyclerView.getViewTreeObserver().addOnPreDrawListener(...) when the activity was created
     * - onCreate() was called before onActivityReenter(...) was called. Without tha code the
     * transition would never end, showing the article image at the top of the screen and part
     * of the article list at the bttom of the screen.
     * Based on (karl's answer)
     *     http://stackoverflow.com/questions/32340565/activitytransition-onactivityreenter-onpredraw-never-called
     *
     * @param requestCode
     * @param data
     */
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int originalCurrentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_ORIGINAL_CURRENT_POSITION);
        int currentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_THIS_CURRENT_POSITION);
        Log.v(TAG, "onActivityReenter - currentPosition/originalCurrentPosition: " + currentPosition + "/" + originalCurrentPosition);
        // make sure AppBar is not extended
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.main_appbar);
        appBarLayout.setExpanded(false);
        if (currentPosition != originalCurrentPosition) {
            if (mStaggeredGridLayoutManager == null) {
                Log.v(TAG, "onActivityReenter -  mRecyclerView.scrollToPosition currentPosition: " + currentPosition);
                mRecyclerView.scrollToPosition(currentPosition);
            } else {
                Log.v(TAG, "onActivityReenter -  mStaggeredGridLayoutManager.scrollToPositionWithOffset currentPosition: " + currentPosition);
                mStaggeredGridLayoutManager.scrollToPositionWithOffset(currentPosition, 20);
            }

        }
        Log.v(TAG, "onActivityReenter -  before postponeEnterTransition");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }
        Log.v(TAG, "onActivityReenter -  after  postponeEnterTransition");
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                Log.v(TAG, "onActivityReenter.onPreDraw - start");
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                Log.v(TAG, "onActivityReenter.onPreDraw - before requestLayout");
                mRecyclerView.requestLayout();
                Log.v(TAG, "onActivityReenter.onPreDraw - after  requestLayout");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startPostponedEnterTransition();
                }
                Log.v(TAG, "onActivityReenter.onPreDraw - after  startPostponedEnterTransition");
                mProgressBarHandler.hide();
                return true;
            }
        });
        // For some reason the problem disappeared. If it happen again, add boolean flag, that is
        // // set in OnCreate(...) and set in onLoadFinished(...) and use in the if statement below

        // TRY AGAIN TOMORROW after the computer is restarted

        // if mStaggeredGridLayoutManager is null, it means that the OnCreate was called and
        // onLoadFinished(...) wasn't
//        if (mStaggeredGridLayoutManager == null) {
//            Log.v(TAG, "onActivityReenter - calling requestLayout");
//            mRecyclerView.requestLayout();
//        }
        Log.v(TAG, "onActivityReenter -  end");
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                if (intent.hasExtra(UpdaterService.EXTRA_NETWORK_PROBLEM)) {
                    mIsRefreshing = false;
                    int networkProblemMessageInt = intent.getIntExtra(UpdaterService.EXTRA_NETWORK_PROBLEM, -1);
                    showSnackBar(networkProblemMessageInt, true);
                } else {
                    mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                }
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        TRANSITION_NAMES = new String[cursor.getCount()];
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        mStaggeredGridLayoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mStaggeredGridLayoutManager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (mIsRefreshing) {
                        showSnackBar(R.string.db_update_in_progress, false);
                        return;
                    }
                    Bundle bundle = null;
                    if (!mIsDetailsActivityStarted) {
                        mIsDetailsActivityStarted = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ActivityOptionsCompat options =
                                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                                            ArticleListActivity.this,
                                            vh.thumbnailView,
                                            vh.thumbnailView.getTransitionName());
                            bundle = options.toBundle();
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                        intent.putExtra(LIST_SELECTED_ARTICLE_POSITION, vh.getAdapterPosition());
                        mProgressBarHandler.show();
                        ActivityCompat.startActivity(ArticleListActivity.this, intent, bundle);
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.thisViewHolderPosition = position;
            String articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            holder.titleView.setText(articleTitle);
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TRANSITION_NAMES[position] = articleTitle;
                holder.thumbnailView.setTransitionName(articleTitle);
                holder.thumbnailView.setTag(articleTitle);
            }
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public int thisViewHolderPosition;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    public void showSnackBar(int msg, boolean showIndefinite) {
        Snackbar
                .make(mCoordinatorlayout, msg, (showIndefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG))
                .setActionTextColor(Color.RED)
                .show(); // Don’t forget to show!
    }
}
