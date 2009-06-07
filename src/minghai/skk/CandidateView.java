/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package minghai.skk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS = -1;

    private SoftKeyboard mService;
    private List<String> mSuggestions;
    private int mSelectedIndex;

    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    
    private Rect mBgPadding;

    private static final int MAX_SUGGESTIONS = 150;
    private static final int SCROLL_PIXELS = 20;
    
    private int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private int[] mWordX = new int[MAX_SUGGESTIONS];

    private static final int X_GAP = 5;
    
    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    
    enum ScrollMode {SCROLLED, NEXT, STOP}
    private ScrollMode mScrolled;

    private int mColorNormal;
    private int mColorRecommended;
    private int mColorOther;
    private int mVerticalPadding;
    private Paint mPaint;

    private int mTargetScrollX;
    
    private int mTotalWidth;
    
    private GestureDetector mGestureDetector;

    private int mCurrentWordIndex;
    private int mScrollX;
    
    private static final int MSG_REMOVE_PREVIEW = 1;
    private static final int MSG_REMOVE_THROUGH_PREVIEW = 2;
    
    Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
          switch (msg.what) {
              case MSG_REMOVE_PREVIEW:
                  mPreviewText.setVisibility(GONE);
                  break;
              case MSG_REMOVE_THROUGH_PREVIEW:
                  mPreviewText.setVisibility(GONE);
                  if (mTouchX != OUT_OF_BOUNDS) {
                      removeHighlight();
                  }
                  break;
          }
          
      }
    };

    private TextView mPreviewText;

    private PopupWindow mPreviewPopup;
    private int mPopupPreviewX;
    private int mPopupPreviewY;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelectionHighlight = context.getResources().getDrawable(
                android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });
        
        LayoutInflater inflate =
          (LayoutInflater) context
                  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPreviewText = (TextView) inflate.inflate(R.layout.candidate_preview, null);
        mPreviewPopup = new PopupWindow(context);
        mPreviewPopup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mPreviewPopup.setContentView(mPreviewText);
        mPreviewPopup.setBackgroundDrawable(null);
   
        Resources r = context.getResources();
        
        setBackgroundColor(r.getColor(R.color.candidate_background));
        
        mColorNormal = r.getColor(R.color.candidate_normal);
        mColorRecommended = r.getColor(R.color.candidate_recommended);
        mColorOther = r.getColor(R.color.candidate_other);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        
        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);
        
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

          @Override
          public void onLongPress(MotionEvent me) {
              if (mSuggestions.size() > 0) {
                  if (me.getX() + mScrollX < mWordWidth[0] && mScrollX < 10) {
                      longPressFirstWord();
                  }
              }
          }

          @Override
          public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                  float distanceX, float distanceY) {
            final int width = getWidth();
            mScrolled = ScrollMode.SCROLLED;
            mScrollX = getScrollX();
            mScrollX += (int) distanceX;
            if (mScrollX < 0) {
              mScrollX = 0;
            }
            if (distanceX > 0 && mScrollX + width > mTotalWidth) {
              mScrollX -= (int) distanceX;
            }
            mTargetScrollX = mScrollX;
            hidePreview();
            invalidate();
            return true;
          }
        });
        
        mBgPadding = new Rect(0, 0, 0, 0);
        
        setHorizontalFadingEdgeEnabled(false);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }
    
    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(SoftKeyboard listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;
        
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        mTotalWidth = 0;
        if (mSuggestions == null) return;

        Drawable bg = getBackground();
        if (bg != null) {
        	bg.getPadding(mBgPadding);
        }

        int x = 0;
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final ScrollMode scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;
        final int y = (int) (((height - paint.getTextSize()) / 2) - paint.ascent());
        
        int count = mSuggestions.size();
        if (count> MAX_SUGGESTIONS) count = MAX_SUGGESTIONS; 
        
        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;

            mWordX[i] = x;
            mWordWidth[i] = wordWidth;
            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && scrolled != ScrollMode.SCROLLED) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                if (i == mService.mChoosedIndex) { 
                    paint.setFakeBoldText(true);
                    paint.setColor(mColorRecommended);
                } else {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(suggestion, x + X_GAP, y, paint);
                paint.setColor(mColorOther); 
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top, 
                        x + wordWidth + 0.5f, height + 1, paint);
                paint.setFakeBoldText(false);
            }
            x += wordWidth;
        }
        mTotalWidth = x;
        int tx = getScrollX();
        int cx = mWordX[mService.mChoosedIndex];

        if (scrolled == ScrollMode.SCROLLED && mTargetScrollX != tx) {
            scrollToTarget();

        } else if (scrolled == ScrollMode.NEXT && cx != tx) {
        	scrollTo(cx, getScrollY());
        	invalidate();
          mScrolled = ScrollMode.STOP;
        }
    }
    
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid) {
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<String>(suggestions);
        }
        mTypedWordValid = typedWordValid;
        scrollTo(0, 0);
        mScrollX = 0;
        mTargetScrollX = 0;

        // Compute the total width
        onDraw(null);
        invalidate();
        requestLayout();
    }

    public void scrollPrev() {
      Log.d("TEST", "scrollPrev(): mSuggestion.size() = " + mSuggestions.size() + " mScrollX = " + mScrollX + " getWidth() = " + getWidth());
      mScrollX = getScrollX();
      int i = 0;
        final int count = mSuggestions.size();
        int firstItem = 0; // Actually just before the first item, if at the boundary
        while (i < count) {
            if (mWordX[i] < mScrollX 
                    && mWordX[i] + mWordWidth[i] >= mScrollX - 1) {
                firstItem = i;
                break;
            }
            i++;
        }
        int leftEdge = mWordX[firstItem] + mWordWidth[firstItem] - getWidth();
        if (leftEdge < 0) leftEdge = 0;
        updateScrollPosition(leftEdge);
        Log.d("TEST", "ScrollPrev() Finished: leftEdge = " + leftEdge);
    }
    
    public void scrollNext() {
      Log.d("TEST", "scrollNext): mSuggestion.size() = " + mSuggestions.size() + " mScrollX = " + mScrollX);
        int i = 0;
        mScrollX = getScrollX();
        int targetX = mScrollX;
        final int count = mSuggestions.size();
        int rightEdge = mScrollX + getWidth();
        while (i < count) {
            if (mWordX[i] <= rightEdge &&
                    mWordX[i] + mWordWidth[i] >= rightEdge) {
                targetX = Math.min(mWordX[i], mTotalWidth - getWidth());
                break;
            }
            i++;
        }
        updateScrollPosition(targetX);
        Log.d("TEST", "scrollNext() Finished: targetX = " + targetX);
    }

    private void updateScrollPosition(int targetX) {
      mScrollX = getScrollX();
        if (targetX != mScrollX) {
            // TODO: Animate
            mTargetScrollX = targetX;
            requestLayout();
            invalidate();
            mScrolled = ScrollMode.SCROLLED;
        }
    }
    
    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        invalidate();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

    	// スクロールした時にはここで処理されて終わりのようだ。ソースの頭で定義している。
        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = ScrollMode.STOP;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE: // よってここのコードは生きていない。使用されない。
            if (y <= 0) {
                // Fling up!?
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                    mSelectedIndex = -1;
                }
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP: // ここは生きている。
            if (mScrolled != ScrollMode.SCROLLED) {
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                }
            }
            mScrolled = ScrollMode.STOP;
            mSelectedIndex = -1;
            removeHighlight();
            requestLayout();
            break;
        }
        return true;
    }
    
    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick 
     * gesture.
     * @param x
     */
    public void takeSuggestionAt(float x) {
        mTouchX = (int) x;
        // To detect candidate
        onDraw(null);
        if (mSelectedIndex >= 0) {
            mService.pickSuggestionManually(mSelectedIndex);
        }
        invalidate();
    }
    
    private void hidePreview() {
      mCurrentWordIndex = OUT_OF_BOUNDS;
      if (mPreviewPopup.isShowing()) {
          mHandler.sendMessageDelayed(mHandler
                  .obtainMessage(MSG_REMOVE_PREVIEW), 60);
      }
  }
  
  private void showPreview(int wordIndex, String altText) {
      int oldWordIndex = mCurrentWordIndex;
      mCurrentWordIndex = wordIndex;
      // If index changed or changing text
      if (oldWordIndex != mCurrentWordIndex || altText != null) {
          if (wordIndex == OUT_OF_BOUNDS) {
              hidePreview();
          } else {
              CharSequence word = altText != null? altText : mSuggestions.get(wordIndex);
              mPreviewText.setText(word);
              mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 
                      MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
              int wordWidth = (int) (mPaint.measureText(word, 0, word.length()) + X_GAP * 2);
              final int popupWidth = wordWidth
                      + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight();
              final int popupHeight = mPreviewText.getMeasuredHeight();
              //mPreviewText.setVisibility(INVISIBLE);
              mPopupPreviewX = mWordX[wordIndex] - mPreviewText.getPaddingLeft() - mScrollX;
              mPopupPreviewY = - popupHeight;
              mHandler.removeMessages(MSG_REMOVE_PREVIEW);
              int [] offsetInWindow = new int[2];
              getLocationInWindow(offsetInWindow);
              if (mPreviewPopup.isShowing()) {
                  mPreviewPopup.update(mPopupPreviewX, mPopupPreviewY + offsetInWindow[1], 
                          popupWidth, popupHeight);
              } else {
                  mPreviewPopup.setWidth(popupWidth);
                  mPreviewPopup.setHeight(popupHeight);
                  mPreviewPopup.showAtLocation(this, Gravity.NO_GRAVITY, mPopupPreviewX, 
                          mPopupPreviewY + offsetInWindow[1]);
              }
              mPreviewText.setVisibility(VISIBLE);
          }
      }
  }


    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }
    
    private void longPressFirstWord() {
      /*
        CharSequence word = mSuggestions.get(0);
        if (mService.addWordToDictionary(word.toString())) {
            showPreview(0, getContext().getResources().getString(R.string.added_word, word));
        }
        */
    }

	public void choose(int choosedIndex) {
		mScrolled = ScrollMode.NEXT;
		invalidate();
	}
}
