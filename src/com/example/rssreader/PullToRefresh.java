package com.example.rssreader;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.example.rssreader.adapters.RssAdapter;

import java.util.HashMap;

public class PullToRefresh extends LinearLayout {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final int PULL_TO_REFRESH = 0;
	private static final int RELEASE_TO_REFRESH = PULL_TO_REFRESH + 1;
	private static final int REFRESHING = RELEASE_TO_REFRESH + 1;

	private static final int EVENT_COUNT = 3;

	// ===========================================================
	// Fields
	// ===========================================================

	private int state = PULL_TO_REFRESH;

	private ListView listView;
	private RelativeLayout header;
	private TextView headerText;
	private ImageView headerImage;
	private Animation flipAnimation, reverseAnimation;

	private int headerHeight;
	private float startY = -1;
	private float startSwipeX = -1;
	private float startSwipeY = -1;
	private Handler handler = new Handler();
	private PullToRefreshAdapter adapter;

	private OnItemClickListener onItemClickListener;
	private OnTouchListener onTouchListener;
	private OnRefreshListener onRefreshListener;

	private float[] lastYs = new float[EVENT_COUNT];
	private boolean canPullDownToRefresh = true;

	private OnTouchListener listViewOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View arg0, MotionEvent arg1) {
			return onListViewTouch(arg0, arg1);
		}

	};

	private Runnable hideHeaderRunnable = new Runnable() {

		@Override
		public void run() {
			hideHeader();
		}

	};

	// ===========================================================
	// Constructors
	// ===========================================================

	public PullToRefresh(Context context) {
		this(context, null);
	}

	public PullToRefresh(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setAdapter(BaseAdapter adapter) {
		this.adapter = new PullToRefreshAdapter(adapter);
		if (adapter == null) {
			listView.setAdapter(null);
		} else {
			listView.setAdapter(this.adapter);
		}

		resetHeader();
	}

	public ListView getListView() {
		return listView;
	}

	public void setSelection(int position) {
		if (state == REFRESHING) {
			position++;
		}

		listView.setSelection(position);
	}

	public void setSelectionFromTop(int position, int y) {
		if (state == REFRESHING) {
			position++;
		}

		listView.setSelectionFromTop(position, y);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		onItemClickListener = listener;

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (onItemClickListener == null) {
					return;
				}
				int offset = 0;
				if (state == REFRESHING) {
					offset = 1;
				}
				onItemClickListener
						.onItemClick(arg0, arg1, arg2 - offset, arg3);
			}

		});
	}

	public void onRefreshComplete() {
		resetHeader();
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		onRefreshListener = listener;
	}

	public int getFirstVisiblePosition() {
		return listView.getFirstVisiblePosition();
	}

	public int getLastVisiblePosition() {
		if (state == REFRESHING) {
			return listView.getLastVisiblePosition() - 1;
		} else {
			return listView.getLastVisiblePosition();
		}
	}

	public void onDestroy() {
		listView = null;
		if (adapter != null) {
			adapter.onDestroy();
			adapter = null;
		}
		onRefreshListener = null;
		onItemClickListener = null;
	}

	public void setRefreshed() {
		state = REFRESHING;

		int topMargin = getHeaderScroll();
		if (topMargin != 0) {
			setHeaderScroll(topMargin - headerHeight);
		}

		header.setVisibility(View.INVISIBLE);

		if (adapter != null) {
			adapter.setRefreshed(true);
		}
	}

	public ListAdapter getAdapter() {
		return listView.getAdapter();
	}

	public void setOnScrollListener(OnScrollListener listener) {
		listView.setOnScrollListener(listener);
	}

	public boolean isListViewShown() {
		return listView.isShown();
	}

	public View getListViewChildAt(int index) {
		if (state == REFRESHING) {
			index++;
		}

		return listView.getChildAt(index);
	}

	public void setEmptyView(View emptyView) {
		listView.setEmptyView(emptyView);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void setOnTouchListener(OnTouchListener listener) {
		onTouchListener = listener;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.VERTICAL);

		header = (RelativeLayout) LayoutInflater.from(context).inflate(
				R.layout.pull_to_refresh_header, this, false);

		headerText = (TextView) header.findViewById(R.id.pull_to_refresh_text);
		headerImage = (ImageView) header
				.findViewById(R.id.pull_to_refresh_image);

		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);

		addView(header, lp);

		measureView(header);
		headerHeight = header.getMeasuredHeight();

		// ListView

		listView = new ListView(context, attrs) {

			@Override
			public BaseAdapter getAdapter() {
				if (adapter != null) {
					return adapter.getInnerAdapter();
				} else {
					return null;
				}
			}

		};
		listView.setId(View.NO_ID);
		listView.setOnTouchListener(listViewOnTouchListener);

		lp = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);

		addView(listView, lp);

		flipAnimation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		flipAnimation.setInterpolator(new LinearInterpolator());
		flipAnimation.setDuration(250);
		flipAnimation.setFillAfter(true);
		reverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		reverseAnimation.setInterpolator(new LinearInterpolator());
		reverseAnimation.setDuration(250);
		reverseAnimation.setFillAfter(true);

		setPadding(getPaddingLeft(), -headerHeight, getPaddingRight(),
				getPaddingBottom());
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	private boolean onListViewTouch(View view, MotionEvent event) {


		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE :

                updateEventStates(event);

				if (isPullingDownToRefresh() && startY == -1) {
					if (startY == -1) {
						startY = event.getY();
					}
					return false;
				}

				if (startY != -1 && !listView.isPressed()) {
					pullDown(event, startY);
					return true;
				}
				break;
			case MotionEvent.ACTION_UP :


                initializeYsHistory();
                startY = -1;

                if (state == RELEASE_TO_REFRESH) {
                    setRefreshed();
                    if (onRefreshListener != null) {
                        onRefreshListener.onRefresh();
                    }
                }

                ensureHeaderPosition();

                // defines swipe left/right action
                if (startSwipeX != -1  && startSwipeY != -1
                        && (startSwipeX - event.getX() < 0)
                        && (Math.abs(startSwipeX - event.getX()) > ViewConfiguration.getTouchSlop() * 2)
                        && (Math.abs(startSwipeY - event.getY()) < 50)) {
                    swipeLeft(view, event);
                    return true;
                } else if(startSwipeX != -1  && startSwipeY != -1
                    && (startSwipeX - event.getX() > 0)
                    && (Math.abs(startSwipeX - event.getX()) > ViewConfiguration.getTouchSlop() * 2)
                        && (Math.abs(startSwipeY - event.getY()) < 50)) {
                        swipeRight(view, event);
                        return true;
                }

                break;
            case MotionEvent.ACTION_DOWN :
                // saves current x position
                startSwipeX = event.getX();
                startSwipeY = event.getY();

                break;
		}

		if (onTouchListener != null) {
			return onTouchListener.onTouch(view, event);
		}
		return false;
	}

	private void resetHeader() {
		state = PULL_TO_REFRESH;
		initializeYsHistory();
		startY = -1;
		header.setVisibility(View.VISIBLE);
		headerText.setText(R.string.pull_to_refresh_pull_label);
		headerImage.clearAnimation();

		setHeaderScroll(0);

		if (adapter != null) {
			adapter.setRefreshed(false);
		}
	}

	private void pullDown(MotionEvent event, float firstY) {
		float averageY = average(lastYs);

		int height = (int) (Math.max(averageY - firstY, 0));

		setHeaderScroll((int) (height));

		if (state == PULL_TO_REFRESH && height - headerHeight > 0) {
			state = RELEASE_TO_REFRESH;
			headerText.setText(R.string.pull_to_refresh_release_label);
			headerImage.clearAnimation();
			headerImage.startAnimation(flipAnimation);
		}
		if (state == RELEASE_TO_REFRESH && height - headerHeight <= 0) {
			state = PULL_TO_REFRESH;
			headerText.setText(R.string.pull_to_refresh_pull_label);
			headerImage.clearAnimation();
			headerImage.startAnimation(reverseAnimation);
		}
	}

	private void setHeaderScroll(int y) {
		scrollTo(0, -y);
	}

	private int getHeaderScroll() {
		return -getScrollY();
	}

	private float average(float[] ysArray) {
		float avg = 0;
		for (int i = 0; i < EVENT_COUNT; i++) {
			avg += ysArray[i];
		}
		return avg / EVENT_COUNT;
	}

	private void initializeYsHistory() {
		for (int i = 0; i < EVENT_COUNT; i++) {
			lastYs[i] = 0;
		}
	}

	private void updateEventStates(MotionEvent event) {
		for (int i = 0; i < EVENT_COUNT - 1; i++) {
			lastYs[i] = lastYs[i + 1];
		}

		float y = event.getY();
		int top = listView.getTop();
		lastYs[EVENT_COUNT - 1] = y + top;
	}

	private boolean isPullingDownToRefresh() {
		return canPullDownToRefresh && state != REFRESHING && isIncremental()
				&& isFirstVisible() && adapter != null
				&& adapter.isAbleToPullToRefresh();
	}

	private boolean isFirstVisible() {
		if (this.listView.getCount() == 0) {
			return true;
		} else if (listView.getFirstVisiblePosition() == 0) {
			return listView.getChildAt(0).getTop() >= listView.getTop();
		} else {
			return false;
		}
	}

	private boolean isIncremental() {
		return this.isIncremental(0, EVENT_COUNT - 1);
	}

	private boolean isIncremental(int from, int to) {
		return lastYs[from] != 0 && lastYs[to] != 0
				&& Math.abs(lastYs[from] - lastYs[to]) > 10
				&& lastYs[from] < lastYs[to];
	}

	private void ensureHeaderPosition() {
		handler.post(hideHeaderRunnable);
	}

	private void hideHeader() {
		int padding = getHeaderScroll();
		if (padding != 0) {
			int top = padding - (int) (padding / 2);
			if (top < 2) {
				top = 0;
			}

			setHeaderScroll(top);

			handler.postDelayed(hideHeaderRunnable, 20);
		}
	}

    private void swipeLeft(View view, MotionEvent event) {
        changeItemState(view, event);
    }

    private void swipeRight(View view, MotionEvent event) {
        changeItemState(view, event);
    }

    private void changeItemState(View view, MotionEvent event)   {
        Integer position = listView.pointToPosition((int) event.getX(), (int) event.getY());
        RssAdapter adapter = (RssAdapter) listView.getAdapter();

        if (position != -1) {
            HashMap item = (HashMap) listView.getAdapter().getItem(position);
            Integer state = (Integer) item.get("state");

            if (state ==  RssAdapter.STATE_EXPANDED) {
                item.put("state", RssAdapter.STATE_NORMAL);
                adapter.updateItem(position, item);
                ((BaseAdapter) adapter).notifyDataSetChanged();
            } else {
                item.put("state", RssAdapter.STATE_EXPANDED);
                adapter.updateItem(position, item);
                ((BaseAdapter) adapter).notifyDataSetChanged();
            }

            listView.setSelection(position);
        }
    }

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public interface OnRefreshListener {

		public void onRefresh();

	}

	private class PullToRefreshAdapter extends BaseAdapter {

		// ===========================================================
		// Constants
		// ===========================================================

		// ===========================================================
		// Fields
		// ===========================================================

		private BaseAdapter adapter;
		private int offset = 0;
		private AdapterSetObserver adapterSetObserver;

		// ===========================================================
		// Constructors
		// ===========================================================

		public PullToRefreshAdapter(BaseAdapter adapter) {
			onDestroy();
			if (adapter == null) {
				return;
			}
			adapterSetObserver = new AdapterSetObserver();
			adapter.registerDataSetObserver(adapterSetObserver);
			this.adapter = adapter;
		}

		// ===========================================================
		// Getter & Setter
		// ===========================================================

		public boolean isAbleToPullToRefresh() {
			if (adapter.getCount() == 0) {
				return false;
			} else {
				return true;
			}
		}

		public void setRefreshed(boolean value) {
			if (value) {
				offset = 1;
			} else {
				offset = 0;
			}
			notifyDataSetChanged();
		}

		public BaseAdapter getInnerAdapter() {
			return adapter;
		}

		public void onDestroy() {
			if (adapter != null) {
				adapter.unregisterDataSetObserver(adapterSetObserver);
				adapter = null;
			}
		}

		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		@Override
		public boolean isEnabled(int position) {
			if ((position == 0 && offset == 1) || adapter == null) {
				return false;
			} else {
				return adapter.isEnabled(position - offset);
			}
		}

		@Override
		public int getCount() {
			if (adapter != null) {
				return adapter.getCount() + offset;
			} else {
				return 0;
			}
		}

		@Override
		public int getViewTypeCount() {
			if (adapter != null) {
				return adapter.getViewTypeCount() + 1;
			} else {
				return 1;
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (adapter == null) {
				return 0;
			}
			if (position == 0 && offset == 1) {
				return adapter.getViewTypeCount();
			}
			return adapter.getItemViewType(position - offset);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if ((position == 0 && offset == 1) || adapter == null) {
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext())
							.inflate(R.layout.pull_to_refresh_header, null);
					convertView.findViewById(R.id.pull_to_refresh_image)
							.setVisibility(View.GONE);
					convertView.findViewById(R.id.pull_to_refresh_progress)
							.setVisibility(View.VISIBLE);
					TextView tv = (TextView) convertView
							.findViewById(R.id.pull_to_refresh_text);
					tv.setText(R.string.pull_to_refresh_refreshing_label);
				}
				return convertView;
			}

			return adapter.getView(position - offset, convertView, parent);
		}

		@Override
		public Object getItem(int position) {
			if ((position == 0 && offset == 1) || adapter == null) {
				return null;
			} else {
				return adapter.getItem(position - offset);
			}
		}

		@Override
		public long getItemId(int position) {
			if ((position == 0 && offset == 1) || adapter == null) {
				return 0;
			} else {
				return adapter.getItemId(position - offset);
			}
		}

		// ===========================================================
		// Methods
		// ===========================================================

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================

		private class AdapterSetObserver extends DataSetObserver {

			@Override
			public void onChanged() {
				PullToRefreshAdapter.this.notifyDataSetChanged();
			}

			@Override
			public void onInvalidated() {
				PullToRefreshAdapter.this.notifyDataSetInvalidated();
			}
		}

	}

}