package com.hardik.tabsflip

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import com.jem.fliptabs.R.drawable
import com.jem.fliptabs.R.layout
import com.jem.fliptabs.R.styleable
import kotlinx.android.synthetic.main.fliptab.view.*

class FlipTab : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(
    context: Context,
    attrs: AttributeSet?
  ) : super(context, attrs) {
    initialize(attrs)
  }

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
  ) : super(
      context,
      attrs,
      defStyleAttr
  ) {
    initialize(attrs)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
  ) : super(context, attrs, defStyleAttr, defStyleRes) {
    initialize(attrs)
  }

  private var isLeftSelected: Boolean = true
  private var animationMiddleViewFlippedFlag: Boolean = false

  private val leftTabText get() = tab_left.text.toString()
  private val rightTabText get() = tab_right.text.toString()

  private var tabSelectedListener: TabSelectedListener? = null

  private val leftSelectedDrawable by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      resources.getDrawable(drawable.tab_left_selected, null)
    } else {
      resources.getDrawable(drawable.tab_left_selected)
    }
  }
  private val rightSelectedDrawable by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      resources.getDrawable(drawable.tab_right_selected, null)
    } else {
      resources.getDrawable(drawable.tab_right_selected)
    }
  }

  companion object {
    private val FLIP_ANIMATION_DURATION = 500
    private val WOBBLE_RETURN_ANIMATION_DURATION = 250
    private val WOBBLE_ANGLE: Float = 5f
    private val OVERALL_COLOR: Int = Color.parseColor("#ff0099cc")
  }

  private var flipAnimationDuration =
    FLIP_ANIMATION_DURATION
  private var wobbleReturnAnimationDuration =
    WOBBLE_RETURN_ANIMATION_DURATION
  private var wobbleAngle = WOBBLE_ANGLE

  init {
    inflate(context, layout.fliptab, this)
    tab_left.setOnClickListener {
      if (isLeftSelected) {
        tabSelectedListener?.onTabReselected(isLeftSelected, leftTabText)
      } else {
        flipTabs()
      }
    }
    tab_right.setOnClickListener {
      if (isLeftSelected) {
        flipTabs()
      } else {
        tabSelectedListener?.onTabReselected(isLeftSelected, rightTabText)
      }
    }
    clipChildren = false
    clipToPadding = false
  }

  private fun initialize(attrs: AttributeSet?) {
    attrs?.let {
      val typedArray = context.obtainStyledAttributes(
          it,
          styleable.FlipTab, 0, 0
      )
      typedArray.apply {
        flipAnimationDuration =
          getInt(
              styleable.FlipTab_flipAnimationDuration,
              FLIP_ANIMATION_DURATION
          )
        wobbleReturnAnimationDuration = getInt(
            styleable.FlipTab_wobbleReturnAnimationDuration,
            WOBBLE_RETURN_ANIMATION_DURATION
        )
        wobbleAngle = getFloat(
            styleable.FlipTab_wobbleAngle,
            WOBBLE_ANGLE
        )
        if (hasValue(styleable.FlipTab_overallColor)) {
          setOverallColor(
              getColor(
                  styleable.FlipTab_overallColor,
                  OVERALL_COLOR
              )
          )
        } else {
          setTextColor(
              getColor(
                  styleable.FlipTab_textColor,
                  OVERALL_COLOR
              )
          )
          setHighlightColor(
              getColor(
                  styleable.FlipTab_highlightColor,
                  OVERALL_COLOR
              )
          )
        }
        if (typedArray.getInt(
                styleable.FlipTab_startingTab, 0
            ) == 1
        ) {
          isLeftSelected = false
          tab_selected_container.rotationY = 180f
          tab_selected.background = rightSelectedDrawable
          tab_selected.scaleX = -1f
        }
        setLeftTabText(
            getString(
                styleable.FlipTab_leftTabText
            ) ?: "Left tab"
        )
        setRightTabText(
            getString(
                styleable.FlipTab_rightTabText
            ) ?: "Right tab"
        )
      }
      typedArray.recycle()
    }
  }

  public fun flipTabs() {
    animationMiddleViewFlippedFlag = false
    isLeftSelected = !isLeftSelected
    tab_selected_container.animate()
        .rotationY(if (isLeftSelected) 0f else 180f)
        .setDuration(flipAnimationDuration.toLong())
        .withStartAction {
          (parent as ViewGroup?)?.clipChildren = false
          (parent as ViewGroup?)?.clipToPadding = false
          tabSelectedListener?.onTabSelected(
              isLeftSelected,
              if (isLeftSelected) leftTabText else rightTabText
          )
        }
        .setUpdateListener {
          if (animationMiddleViewFlippedFlag) return@setUpdateListener

          //TODO: Find out a better alternative to changing Background in the middle of animation (might result in dropped frame/stutter)
          if (isLeftSelected && tab_selected_container.rotationY <= 90f) {
            animationMiddleViewFlippedFlag = true
            tab_selected.text = leftTabText
            tab_selected.background = leftSelectedDrawable
            tab_selected.scaleX = 1f
          } else if (!isLeftSelected && tab_selected_container.rotationY >= 90f) {
            animationMiddleViewFlippedFlag = true
            tab_selected.text = rightTabText
            tab_selected.background = rightSelectedDrawable
            tab_selected.scaleX = -1f
          }
        }
        .withEndAction {
          (parent as ViewGroup?)?.clipChildren = true
          (parent as ViewGroup?)?.clipToPadding = true
        }
        .start()
    val animSet = AnimatorSet()
    val animator1 = ObjectAnimator.ofFloat(
        base_fliptab_container, "rotationY", if (isLeftSelected) -wobbleAngle else wobbleAngle
    )
    animator1.duration = flipAnimationDuration.toLong()
    val animator2 = ObjectAnimator.ofFloat(base_fliptab_container, "rotationY", 0f)
    animator2.duration = wobbleReturnAnimationDuration.toLong()
    animSet.playSequentially(animator1, animator2)
    animSet.start()
  }

  public interface TabSelectedListener {
    public fun onTabSelected(
      isLeftTab: Boolean,
      tabTextValue: String
    ): Unit

    public fun onTabReselected(
      isLeftTab: Boolean,
      tabTextValue: String
    ): Unit
  }

  public fun setTabSelectedListener(tabSelectedListener: TabSelectedListener) {
    this.tabSelectedListener = tabSelectedListener
  }

  public fun setWobbleAngle(angle: Float) {
    wobbleAngle = angle
  }

  public fun setWobbleReturnAnimationDuration(duration: Int) {
    wobbleReturnAnimationDuration = duration
  }

  public fun setFlipAnimationDuration(duration: Int) {
    flipAnimationDuration = duration
  }

  public fun setOverallColor(color: Int) {
    setTextColor(color)
    setHighlightColor(color)
  }

  public fun setTextColor(color: Int) {
    tab_left.setTextColor(color)
    tab_right.setTextColor(color)
  }

  public fun setHighlightColor(color: Int) {
    (tab_left.background as GradientDrawable)?.setStroke(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            resources.displayMetrics
        ).toInt(), color
    )
    (tab_right.background as GradientDrawable)?.setStroke(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            resources.displayMetrics
        ).toInt(), color
    )
    (tab_selected.background as GradientDrawable)?.setStroke(
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            resources.displayMetrics
        ).toInt(), color
    )
    (tab_selected.background as GradientDrawable)?.setColor(color)
    DrawableCompat.setTint(leftSelectedDrawable, color)
    DrawableCompat.setTint(rightSelectedDrawable, color)
  }

  public fun setLeftTabText(text: String) {
    tab_left.text = text
    if (isLeftSelected) {
      tab_selected.text = text
    }
  }

  public fun setRightTabText(text: String) {
    tab_right.text = text
    if (!isLeftSelected) {
      tab_selected.text = text
    }
  }

  public fun selectLeftTab(withAnimation: Boolean) {
    if (!isLeftSelected) {
      if (withAnimation) {
        flipTabs()
      } else {
        isLeftSelected = true
        tab_selected_container.rotationY = 0f
        tab_selected.text = leftTabText
        tab_selected.background = leftSelectedDrawable
        tab_selected.scaleX = 1f
        tabSelectedListener?.onTabSelected(isLeftSelected, leftTabText)
      }
    } else {
      tabSelectedListener?.onTabReselected(isLeftSelected, leftTabText)
    }
  }

  public fun selectRightTab(withAnimation: Boolean) {
    if (isLeftSelected) {
      if (withAnimation) {
        flipTabs()
      } else {
        isLeftSelected = false
        tab_selected_container.rotationY = 180f
        tab_selected.text = rightTabText
        tab_selected.background = rightSelectedDrawable
        tab_selected.scaleX = -1f
        tabSelectedListener?.onTabSelected(isLeftSelected, rightTabText)
      }
    } else {
      tabSelectedListener?.onTabReselected(isLeftSelected, rightTabText)
    }
  }
}