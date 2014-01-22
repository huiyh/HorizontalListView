package com.hyh.widget;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;

/**
 * @author hyh 
 * creat_at：2014-1-17-上午10:31:55
 */
public class HListView extends AdapterView<ListAdapter> implements OnGestureListener {
	private static final String BUNDLE_ID_CURRENT_X = "Store_current_X";
	private static final String BUNDLE_ID_PARENT_STATE = "parent_state";


	private List<Queue<View>> mRecycledViewCache = new ArrayList<Queue<View>>();

	
	/**左右发光效果*/
	private EdgeEffectCompat mEdgeEffectLeft, mEdgeEffectRight;
	private ListAdapter mAdatper;
	private GestureDetector mGestureDetector;


	protected boolean mHasNotifiedRunningLowOnData;
	private boolean mDataChanged;
	private DataSetObserver mAdapterObserver = new DataSetObserver(){


		/**继承自AdapterView*/
		@Override
		public void onChanged() {
			mDataChanged = true;
			mHasNotifiedRunningLowOnData = false;
			unpressTouchedChild();
			
			invalidate();
			requestLayout();
		}

		/**继承自AdapterView*/
		@Override
		public void onInvalidated() {
			mHasNotifiedRunningLowOnData = false;
			unpressTouchedChild();
			resetView();
			
			invalidate();
			requestLayout();
		}
		
	};


	private int mSelectedAdapterIndex;


	private int mLeftAdapterIndex;


	private int mRightAdapterIndex;


	private int mDisplayOffset;


	private int mCurrentX;


	private int mNextX;


	private int mMaxX;


	private int mCurrentScrollState;


	private OnScrollStateChangedListener mOnScrollStateChangedListener;


	private View mViewBeingTouched;


	private int mHeightMeasureSpec;


	private Integer mStoredX;
	private Scroller mScroller = new Scroller(getContext());

	public HListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mEdgeEffectLeft = new EdgeEffectCompat(context);
		mEdgeEffectRight = new EdgeEffectCompat(context);
		mGestureDetector = new GestureDetector(context,this);
		initView();
	}
	


	private void initView() {
		mLeftAdapterIndex = -1;
		mRightAdapterIndex = -1;
		mDisplayOffset = 0;
		mCurrentX = 0;
		mNextX = 0;
		mMaxX = Integer.MAX_VALUE;
		setCurrentScrollState(OnScrollStateChangedListener.SCROLL_STATE_IDLE);// TODO
	}
	
	/**将HListVIew恢复到初始化时的状态*/
	private void resetView(){
		initView();
		removeAllViewsInLayout();
		requestLayout();
	}
	
	
	/**继承自View*/	
	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putInt(BUNDLE_ID_CURRENT_X, mCurrentX);
		bundle.putParcelable(BUNDLE_ID_PARENT_STATE, super.onSaveInstanceState());
		return bundle;
	}

	/**继承自View*/
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if(state instanceof Bundle){
			Bundle bundle = (Bundle)state;
			mStoredX = Integer.valueOf(bundle.getInt(BUNDLE_ID_CURRENT_X));
			super.onRestoreInstanceState(bundle.getParcelable(BUNDLE_ID_PARENT_STATE));
		}
	}

	/**继承自AdapterView*/
	@Override
	public ListAdapter getAdapter() {
		return mAdatper;
	}

	
	
	/**继承自AdapterView*/
	@Override
	public void setAdapter(ListAdapter adapter) {
		if(mAdatper != null){
			mAdatper.unregisterDataSetObserver(mAdapterObserver); 
		}
		if(adapter != null){
			mAdatper = adapter;
			mAdatper.registerDataSetObserver(mAdapterObserver);
			initRecycledViewCache(mAdatper.getViewTypeCount());
		}
		// TODO 刷新界面

	}

	/**继承自AdapterView*/
	@Override
	public View getSelectedView() {
		return getItemView(mSelectedAdapterIndex);
	}

	/**继承自AdapterView*/
	@Override
	public void setSelection(int position) {
		mSelectedAdapterIndex = position;
	}

	/**继承自AdapterView*/
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if(mAdatper == null){
			return;
		}
		
		invalidate();
		
		if(mDataChanged){
			int oldCurrentX = mCurrentX;
			initView();
			removeAllViews();
			mNextX = oldCurrentX;
			mDataChanged = false;
		}
		
		if(mStoredX != null){
			mNextX = mStoredX;
			mStoredX = null;
		}
		
		if(mScroller.computeScrollOffset()){
			mNextX = mScroller.getCurrX();
		}
		
		if(mNextX < 0){
			mNextX = 0;
			
			if(mEdgeEffectLeft.isFinished()){
				// TODO mEdgeEffectLeft.onAbsorb(velocity)
			}
			
			mScroller.forceFinished(true);
			setCurrentScrollState(OnScrollStateChangedListener.SCROLL_STATE_IDLE);
		}else if(mNextX > mMaxX){
			 mNextX = mMaxX;
			 
			 if(mEdgeEffectRight.isFinished()){
				 // TODO
			 }
			 mScroller.forceFinished(true);
			 
		}
		/*
		 *X 左边为0 
		 */
		int dx = mCurrentX - mNextX;
		removeNoVisibleChileren(dx);
	}
	
	private void removeNoVisibleChileren(int dx) {
		
	}



	/**继承自 OnGestureListener*/
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**继承自 OnGestureListener*/
	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**继承自 OnGestureListener*/
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/**继承自 OnGestureListener*/
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	/**继承自 OnGestureListener*/
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**继承自 OnGestureListener*/
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}
	

	private View getItemView(int adapterIndex) {
		if(!(adapterIndex < mLeftAdapterIndex || adapterIndex > mRightAdapterIndex)){
			return getChildAt(adapterIndex - mLeftAdapterIndex);
		}
		return null;
	}

	/**初始化被回收的itemView*/
	private void initRecycledViewCache(int viewTypeCount) {
		mRecycledViewCache.clear();
		for(int i=0;i<viewTypeCount;i++){
			mRecycledViewCache.add(new LinkedList<View>());
		}
	}
	
	private View getRecycledView(int adapterIndex){
		int itemViewType = mAdatper.getItemViewType(adapterIndex);
		if(isItemViewTypeValid(itemViewType)){
			return mRecycledViewCache.get(itemViewType).poll();
		}
		return null;
	}
	
	private void recycleView(int adapterIndex,View view){
		int itemViewType = mAdatper.getItemViewType(adapterIndex);
		if(isItemViewTypeValid(adapterIndex)){
			mRecycledViewCache.get(itemViewType).offer(view);
		}
	}
	
	private boolean isItemViewTypeValid(int itemViewType) {
		return mRecycledViewCache != null && mRecycledViewCache.size()> itemViewType;
	}

	private void addAndMensureChild(View child,int index){
		LayoutParams params = getLayoutParams(child); 
		addViewInLayout(child, index, params);
		measureChild(child);
	}
	
	private ViewGroup.LayoutParams getLayoutParams(View child) {
		ViewGroup.LayoutParams params = child.getLayoutParams();
		if(null == params){
			params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		}
		return params;
	}

	private void measureChild(View child) {
		ViewGroup.LayoutParams layoutParams = getLayoutParams(child);
		int heightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec, getPaddingTop() + getPaddingBottom(), layoutParams.height);
		int widthSpec = 0;
		if(layoutParams.width > 0){
			widthSpec = MeasureSpec.makeMeasureSpec(widthSpec, MeasureSpec.EXACTLY);
		}else{
			widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(widthSpec, heightSpec);
	}

	




	protected void unpressTouchedChild() {
		if(mViewBeingTouched == null){
			mViewBeingTouched.setPressed(false);
			refreshDrawableState();
			
			mViewBeingTouched = null;
		}
	}
	
	private void setCurrentScrollState(int scrollState) {
		if(mCurrentScrollState != scrollState  && mOnScrollStateChangedListener != null){
			mOnScrollStateChangedListener.onScrollStateChanged(scrollState);
		}
		mCurrentScrollState = scrollState;
	}
	
	public OnScrollStateChangedListener getmOnScrollStateChangedListener() {
		return mOnScrollStateChangedListener;
	}
	public void setmOnScrollStateChangedListener(OnScrollStateChangedListener mOnScrollStateChangedListener) {
		this.mOnScrollStateChangedListener = mOnScrollStateChangedListener;
	}
	
	public interface OnScrollStateChangedListener{
		public static final int SCROLL_STATE_IDLE = 0;
		public static final int SCROLL_STATE_TOUCH_SCROLL = 1;
		public static final int SCROLL_STATE_FLING = 2;
		public void onScrollStateChanged(int scrollState);
	}
}
