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

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private View mCoordinatorlayout;
    private boolean mIsDetailsActivityStarted;
    public static String[] TRANSITION_NAMES;
    private Bundle mTmpReenterState;
    private SharedElementCallback mCallback = null;

    public static final String LIST_SELECTED_ARTICLE_POSITION = "com.example.xyzreader.ui.LIST_SELECTED_ARTICLE_POSITION";

    private static final String TAG = ArticleListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_article_list);
        setContentView(R.layout.activity_article_list_with_coordinatorlayout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final SharedElementCallback mCallback = new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    Log.v(TAG,"onMapSharedElements - start - mTmpReenterState: " + mTmpReenterState);
                    if (mTmpReenterState != null) {
                        int originalCurrentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_ORIGINAL_CURRENT_POSITION);
                        int currentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_THIS_CURRENT_POSITION);
                        if (originalCurrentPosition != currentPosition) {
                            // If startingPosition != currentPosition the user must have swiped to a
                            // different page in the DetailsActivity. We must update the shared element
                            // so that the correct one falls into place.
                            String newTransitionName = TRANSITION_NAMES[currentPosition];
                            View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                            Log.v(TAG,"onMapSharedElements - originalCurrentPosition/currentPosition: " + originalCurrentPosition + "/" + currentPosition);
                            Log.v(TAG,"onMapSharedElements - newTransitionName/newSharedElement: " + newTransitionName + "/" + newSharedElement);
                            if (newSharedElement != null) {
                                names.clear();
                                names.add(newTransitionName);
                                sharedElements.clear();
                                sharedElements.put(newTransitionName, newSharedElement);
                            }
                        }

                        mTmpReenterState = null;
                    } else {
                        Log.v(TAG,"onMapSharedElements - the activity is exiting");
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
//            defineCallback();
            setExitSharedElementCallback(mCallback);
        }

        mCoordinatorlayout = findViewById(R.id.articleListActivity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        final View toolbarContainerView = findViewById(R.id.toolbar_container);

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
    }

//    private void defineCallback() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            final SharedElementCallback mCallback = new SharedElementCallback() {
//                @Override
//                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
//                    Log.v(TAG,"onMapSharedElements - start - mTmpReenterState: " + mTmpReenterState);
//                    if (mTmpReenterState != null) {
//                        int originalCurrentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_ORIGINAL_CURRENT_POSITION);
//                        int currentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_THIS_CURRENT_POSITION);
//                        if (originalCurrentPosition != currentPosition) {
//                            // If startingPosition != currentPosition the user must have swiped to a
//                            // different page in the DetailsActivity. We must update the shared element
//                            // so that the correct one falls into place.
//                            String newTransitionName = TRANSITION_NAMES[currentPosition];
//                            View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
//                            Log.v(TAG,"onMapSharedElements - originalCurrentPosition/currentPosition: " + originalCurrentPosition + "/" + currentPosition);
//                            Log.v(TAG,"onMapSharedElements - newTransitionName/newSharedElement: " + newTransitionName + "/" + newSharedElement);
//                            if (newSharedElement != null) {
//                                names.clear();
//                                names.add(newTransitionName);
//                                sharedElements.clear();
//                                sharedElements.put(newTransitionName, newSharedElement);
//                            }
//                        }
//
//                        mTmpReenterState = null;
//                    } else {
//                        Log.v(TAG,"onMapSharedElements - the activity is exiting");
//                        // If mTmpReenterState is null, then the activity is exiting.
//                        View navigationBar = findViewById(android.R.id.navigationBarBackground);
//                        View statusBar = findViewById(android.R.id.statusBarBackground);
//                        if (navigationBar != null) {
//                            names.add(navigationBar.getTransitionName());
//                            sharedElements.put(navigationBar.getTransitionName(), navigationBar);
//                        }
//                        if (statusBar != null) {
//                            names.add(statusBar.getTransitionName());
//                            sharedElements.put(statusBar.getTransitionName(), statusBar);
//                        }
//                    }
//                }

//                @Override
//                public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
//                    Log.v(TAG,"onSharedElementStart - start");
//                    super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
//                }
//
//                @Override
//                public void onSharedElementEnd(List<String> sharedElementNames,
//                                               List<View> sharedElements, List<View> sharedElementSnapshots) {
//                    Log.v(TAG,"onSharedElementEnd - start");
//                    super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
//                }
//            };
//        }
//    }

    /**
     * Start new search.
     */
    private void refresh() {
//        Log.v(TAG, "refresh - called - mIsRefreshing: " + mIsRefreshing);
        if (!mIsRefreshing) {
            startService(new Intent(this, UpdaterService.class));
        } else {
//            Log.v(TAG, "refresh - refreshing skipped - mIsRefreshing: " + mIsRefreshing);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }
//http://www.androiddesignpatterns.com/2015/03/activity-postponed-shared-element-transitions-part3b.html

    //http://stackoverflow.com/questions/28975840/feature-activity-transitions-vs-feature-content-transitions

    //android developers FEATURE_ACTIVITY_TRANSITIONS
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int originalCurrentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_ORIGINAL_CURRENT_POSITION);
        int currentPosition = mTmpReenterState.getInt(ArticleDetailActivity.EXTRA_THIS_CURRENT_POSITION);
        Log.v(TAG, "onActivityReenter - originalCurrentPosition/currentPosition/startId/selectedId : " + originalCurrentPosition + "/" + currentPosition);
        if (currentPosition != originalCurrentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.v(TAG, "onPreDraw - calling startPostponedEnterTransition");
                    startPostponedEnterTransition();
                }
                return true;
            }
        });
    }

    private boolean mIsRefreshing = false;

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
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
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
                        // FIXME: 26/05/2016 - the SnackBar below should disappear after few seconds
                        showSnackBar(R.string.db_update_in_progress, false);
                        return;
                    }
                    Bundle bundle = null;
                    if (!mIsDetailsActivityStarted) {
                        mIsDetailsActivityStarted = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Log.v(TAG, "onCreateViewHolder - starting activity with ActivityOptions.makeSceneTransitionAnimation");
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
            holder.titleView.setText(position + "-" + articleTitle);
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
