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
	/**最左边的child左边相应parent的X坐标*/
	private int mDividerWidth;

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
		fillList(dx);
	}
	
	private void fillList(int dx) {
		int edge = 0;
		View child = getRightmostsChild();
		if(child != null){
			edge = child.getRight();
		}
		fillListRight(edge,dx);
		
		edge = 0;
		child = getLeftMostChild();
		if(null != child){
			edge = child.getLeft();
		}
		fillListLeft(edge, dx);
		
	}



	private void fillListLeft(int leftEdge, int dx) {
		while(leftEdge + dx - mDisplayOffset > 0 && mLeftAdapterIndex > 0){
			--mLeftAdapterIndex;
			View child = mAdatper.getView(mLeftAdapterIndex, getRecycledView(mLeftAdapterIndex), this);
			addAndMensureChild(child, 0);
			leftEdge -= mLeftAdapterIndex == 0 ? child.getMeasuredWidth() : child.getMeasuredWidth() + mDividerWidth;
			mDisplayOffset -= leftEdge + dx == 0 ? child.getMeasuredWidth() : child.getMeasuredWidth() + mDividerWidth;
		}
	}
	private void fillListRight(int rightEdge, int dx) {
		while(rightEdge + dx + mDisplayOffset < getWidth() && mRightAdapterIndex + 1 < mAdatper.getCount()){
			++mRightAdapterIndex;
			if(mLeftAdapterIndex < 0){
				mLeftAdapterIndex = mRightAdapterIndex;
			}
			View child = mAdatper.getView(mRightAdapterIndex, getRecycledView(mRightAdapterIndex), this);
			addAndMensureChild(child, -1);
			rightEdge += (mRightAdapterIndex == 0 ? 0 : mDividerWidth)+child.getMeasuredWidth();
			determineIfLowOnData();
		}
	}

	private void postitonChildren(final int dx){
		final int childCount = getChildCount();
		if(childCount > 0){
			mDisplayOffset += dx;
			int leftOffset = mDisplayOffset;
			for(int i = 0; i < childCount; i++){
				View child = getChildAt(i);
				int left = leftOffset + getPaddingLeft();
				int top = getPaddingTop();
				int right = left + child.getMeasuredWidth();
				int botton = top + child.getMeasuredHeight();
				
				child.layout(left, top, right, botton);
				
				leftOffset += child.getMeasuredWidth() + mDividerWidth;
			}
		}
		
	}

	private void determineIfLowOnData() {
		// TODO Auto-generated method stub
		
	}



	private void removeNoVisibleChileren(int dx) {
		View child = getLeftMostChild();
		while(null != child && child.getRight() + dx < 0){
//			应用是 mDisplayOffset -= isLastItemInAdapter(mLeftAdapterIndex) ? -(child.getMeasuredWidth()) : -(child.getMeasuredWidth() + mDividerWidth);
//			简写为如下
			mDisplayOffset += isLastItemInAdapter(mLeftAdapterIndex) ? child.getMeasuredWidth() : child.getMeasuredWidth() + mDividerWidth;
			recycleView(mLeftAdapterIndex, child);
			removeViewInLayout(child);
			mLeftAdapterIndex++;
			child = getLeftMostChild();
		}
		
		child = getRightmostsChild();
		while(null != child && child.getLeft() -dx > Integer.MAX_VALUE){
			recycleView(mRightAdapterIndex, child);
			removeViewInLayout(child);
			--mRightAdapterIndex;
			child = getRightmostsChild();
		}
	}




	private View getLeftMostChild() {
		return getChildAt(0);
	}
	
	private View getRightmostsChild() {
		return getChildAt(getChildCount() - 1);
	}

	private View getChild(int adapterIndex){
		if(adapterIndex >= mLeftAdapterIndex && adapterIndex <= mRightAdapterIndex){
			return getChildAt(adapterIndex - mLeftAdapterIndex);
		}
		return null;
	}
	private boolean isLastItemInAdapter(int mLeftAdapterIndex2) {
		// TODO Auto-generated method stub
		return false;
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
	/**
	 * 
	 * @param adapterIndex 根据该index从mAdapter中获取ItemType,用于放入对应的缓存
	 * @param view 待回收的Item
	 */
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
