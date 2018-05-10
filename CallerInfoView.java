
package com.meizu.incallui.widget;

import com.android.incallui.Call;
import com.android.incallui.Log;
import com.android.incallui.R;
import com.meizu.incallui.ui.InCallTypeface;
import com.meizu.incallui.ui.SimSlotIndicatorHelper;
import com.meizu.incallui.utils.DbgUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 采用SpannableString定制TextView显示CallerInfo。依次显示：
 * <p>
 * 头像
 * </p>
 * <p>
 * 第一行： 姓名或号码 / 扩展标签(专线、网络) / SIM卡标识
 * </p>
 * <p>
 * 第二行： 号码 +（归属地，备注，黄页信息），或留言录音状态 。
 * </p>
 *
 * @author Depp 20171026
 */
public abstract class CallerInfoView extends TextView {
    private final String TAG = getClass().getSimpleName();

    /**
     * 文字高亮时颜色
     */
    protected ColorStateList mHighLightColor = ColorStateList.valueOf(Color.WHITE);
    /**
     * 文字正常颜色
     */
    protected ColorStateList mNormalColor = ColorStateList.valueOf(Color.GRAY);

    /**
     * 高亮显示名字
     */
    protected boolean mHighLightName = true;

    /** 定义第一行文字大小：姓名，号码 */
    protected int mFirstLineTextSize;
    /** 定义第二行文字大小：号码，归属地（备注） */
    protected int mSecondLineTextSize;

    /** 姓名 */
    protected String mName;
    /** 号码 */
    protected String mNumber;
    /** 备注 */
    protected String mCallerLabel;
    /** 归属地 */
    protected String mLocation;
    /** 卡信息 */
    protected int mSlotId = -1;
    /** SIM卡图标 */
    protected Drawable mSimIndicator;
    // FLYME:zhulanting@Incallui: [#691642, 2018/1/15] {@
    /** 视频电话拨号时通话状态 */
    protected String mVideoCallDialingStatus;
    // @}

    /** 扩展标签：网络电话/专线电话标识 */
    protected String mExtraIconText;

    protected int mExtraIconColor = Color.WHITE;
    protected int mExtraIconBgColor = Color.GREEN;

    /** Text显示最大宽度, Text + extraIcon + simIcon若超出此宽度则应该省略 */
    protected int mMaxTextContentWidth;
    public CallerInfoView(Context context) {
        super(context);
        init(null);
    }

    public CallerInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CallerInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CallerInfoView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        Resources res = getContext().getResources();

        final int defFirstLineTextSize = res.getDimensionPixelSize(R.dimen.text_size_big);
        final int defSecondLineTextSize = res.getDimensionPixelSize(R.dimen.text_size_secondary);
        mFirstLineTextSize = defFirstLineTextSize;
        mSecondLineTextSize = defSecondLineTextSize;
        // if (attrs == null) {
        // mFirstLineTextSize = defFirstLineTextSize;
        // mSecondLineTextSize = defSecondLineTextSize;
        // } else {
        // TypedArray ta = getContext().obtainStyledAttributes(attrs,
        // R.styleable.CallerInfoTextSize);
        // mFirstLineTextSize = ta.getInteger(R.attr.firstLineTextSize, defFirstLineTextSize);
        // mSecondLineTextSize = ta.getInteger(R.attr.secondLineTextSize, defSecondLineTextSize);
        // ta.recycle();
        // }

        mHighLightColor = ColorStateList.valueOf(Color.WHITE);
        mNormalColor = ColorStateList.valueOf(res.getColor(R.color.incall_text_color_secondary));
    }

    public void setInfos(String name, String number, String location, int slotId) {
        boolean infoChanged = false;
        if (!isSame(name, mName)) {
            infoChanged = true;
            mName = name;
        }
        if (!isSame(number, mNumber)) {
            infoChanged = true;
            mNumber = number;
        }
        if (!isSame(location, mLocation)) {
            infoChanged = true;
            mLocation = location;
        }
        if (mSlotId != slotId) {
            updateSimIndicator(slotId);
            infoChanged |= mSimIndicator != null;
        }

        if (infoChanged) {
            log("setInfos name:" + name + ", number:" + number
                    + ", location:" + location + ", slot:" + slotId);
            buildText();
        }
    }

    /**
     * @param name 名字
     * @param number 号码
     * @param location 归属地
     * @param label 标签
     * @param slotId  卡槽
     */
    public void setInfos(String name, String number, String location, String label, int slotId) {
        log(new StringBuilder("setInfos (").append(name).append(", ").append(number).append(", ")
                .append(location).append(", ").append(label).append(")").toString());

        boolean infoChanged = false;
        if (!isSame(name, mName)) {
            infoChanged = true;
            mName = name;
        }
        if (!isSame(number, mNumber)) {
            infoChanged = true;
            mNumber = number;
        }
        if (!isSame(location, mLocation)) {
            infoChanged = true;
            mLocation = location;
        }
        if (!isSame(label, mCallerLabel)) {
            infoChanged = true;
            mCallerLabel = label;
        }
        if (mSlotId != slotId) {
            mSlotId = slotId;
            if (SimSlotIndicatorHelper.okToShowSimSlotIndicator()) {
                infoChanged = true;
                mSimIndicator = SimSlotIndicatorHelper.getSimIndicatorDrawable(mSlotId);
                if (mSimIndicator != null) {
                    mSimIndicator.setBounds(0, 0, mSimIndicator.getIntrinsicWidth(),
                            mSimIndicator.getIntrinsicHeight());
                }
            } else {
                mSimIndicator = null;
            }
        }
        if (infoChanged) {
            log("setInfos name:" + name + ", number:" + number + ", label:" + mCallerLabel
                    + ", location:" + location + ", slot:" + slotId);
            buildText();
        }
    }

    /**
     * Call #invalidateCallerInfoView() after setting all values
     * @param name 姓名
     */
    public void setName(String name) {
        if (isSame(name, mName)) {
            return;
        }

        mName = name;
        buildText();
    }

    /**
     * Call #invalidateCallerInfoView() after setting all values
     * @param number 号码
     */
    public void setNumber(String number) {
        if (isSame(number, mNumber)) {
            return;
        }

        mNumber = number;
        buildText();
    }

    /** 备注、标签信息 */
    public void setLabel(String label) {
        if (isSame(label, mCallerLabel)) {
            return;
        }

        mCallerLabel = label;
        buildText();
    }

    /**
     * Call #invalidateCallerInfoView() after setting all values
     * @param location 归属地
     */
    public void setLocation(String location) {
        if (isSame(location, mLocation)) {
            return;
        }

        mLocation = location;
        buildText();
    }

    // FLYME:zhulanting@Incallui: [#691642, 2018/1/15] {@
    public void setVideoCallDialingStatus(String callStateLabel) {
        if (isSame(callStateLabel, mVideoCallDialingStatus)) {
            return;
        }

        mVideoCallDialingStatus = callStateLabel;
        buildText();
    }
    // @}

    /**
     * 有效的slotId会显示卡标识
     * @param slot
     */
    public void setSimIndicator(int slotId) {
        if (mSlotId != slotId) {
            updateSimIndicator(slotId);
            if (mSimIndicator != null) {
                buildText();
            }
        }
    }
    private void updateSimIndicator(int slotId) {
        if (mSlotId != slotId) {
            mSlotId = slotId;
            if (SimSlotIndicatorHelper.okToShowSimSlotIndicator()) {
                mSimIndicator = SimSlotIndicatorHelper.getSimIndicatorDrawable(mSlotId);
                if (mSimIndicator != null) {
                    mSimIndicator.setBounds(0, 0, mSimIndicator.getIntrinsicWidth(),
                            mSimIndicator.getIntrinsicHeight());
                }
            } else {
                mSimIndicator = null;
            }
        }
    }

    /** 扩展标签：网络电话/专线电话标识 */
    public void setExtraIconText(Call call, boolean refresh) {
        String text = getExtraIconText(call);
        if (isSame(text, mExtraIconText)) {
            return;
        }

        mExtraIconText = text;
        if (refresh) { // 此方法通常都在setInfos()之前调用, setInfos()中会buildText
            buildText();
        }
    }

    /** 扩展标签文字颜色 */
    public void setExtraIconColor(int color) {
        if (mExtraIconColor == color) {
            return;
        }

        mExtraIconColor = color;
        buildText();
    }

    /** 用于动画过程中，更新字体大小： 即刻刷新view */
    public void setFirstLineTextSize(int firstLineTextSize) {
        if (mFirstLineTextSize == firstLineTextSize) {
            return;
        }
        mFirstLineTextSize = firstLineTextSize;
        buildText();
    }

    /** 用于动画过程中，更新字体大小： 即刻刷新view */
    public void setSecondLineTextSize(int secondLineTextSize) {
        if (mSecondLineTextSize == secondLineTextSize) {
            return;
        }
        mSecondLineTextSize = secondLineTextSize;
        buildText();
    }

    public void setTextSizeToDefault() {
        final int defFirstLineTextSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.text_size_big);
        final int defSecondLineTextSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.text_size_secondary);
        mFirstLineTextSize = defFirstLineTextSize;
        mSecondLineTextSize = defSecondLineTextSize;
        buildText();
    }

    /** 扩展标签背景颜色 */
    public void setExtraIconBgColor(int color) {
        if (mExtraIconBgColor == color) {
            return;
        }

        mExtraIconBgColor = color;
        buildText();
    }

    /** 隐藏文字区域 */
    public void hideText() {
        if (!TextUtils.isEmpty(getText())) {
            setText("");
        }
    }

    /** 显示文字区域 */
    public void buildText() {
        String firstLineText = getFirstLineText();
        if (TextUtils.isEmpty(firstLineText)) {
            if (!TextUtils.isEmpty(getText())) {
                setText("");
            }
            return;
        }
        log("buildText");

        String secondLineText = getSecondLineText();

        SpannableString lineOne = new SpannableString(getFirstLineDisplayText(firstLineText));
        SpannableString lineTwo = TextUtils.isEmpty(secondLineText) ? null
                : new SpannableString(getSecondaryDisplayText(secondLineText));
        lineOne.setSpan(getFirstLineTextSpan(mHighLightName), 0,
                lineOne.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 设置第一行显示内容
        setText(lineOne);

        // 专线电话，网络电话标签icon
        if (!TextUtils.isEmpty(mExtraIconText)) {
            append(buildExtraIcon());
        }

        // 显示卡图标
        if (mSimIndicator != null) {
            SpannableString simSpann = new SpannableString("slot");
            simSpann.setSpan(new TopPaddingImageSpan(mSimIndicator,
                    getSimIconTopMargin(), getExtraIconLeftMargin()),
                    0, simSpann.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            append(simSpann);
        }

        if (lineTwo != null) {
            append("\n");
            lineTwo.setSpan(
                    new TextAppearanceSpan(InCallTypeface.getDefaultTypeface(),
                            Typeface.NORMAL, mSecondLineTextSize,
                            mNormalColor, mNormalColor),
                    0, lineTwo.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // 设置第二行显示内容
            append(lineTwo);
        }
    }

    protected TextAppearanceSpan getFirstLineTextSpan(boolean highlight) {
        ColorStateList color = highlight ? mHighLightColor : mNormalColor;
        return new TextAppearanceSpan(InCallTypeface.getMediumTypeface(),
                Typeface.NORMAL, mFirstLineTextSize, color, color);
    }

    /** ExtraIcon为文字加圆角矩形边框组成, @see RadiusBackgroundSpan */
    private SpannableString buildExtraIcon() {
        // 设置文字大小
        SpannableString extraIcon = new SpannableString(mExtraIconText);
        extraIcon.setSpan(new AbsoluteSizeSpan(getDimenPx(R.dimen.mz_callcard_extra_icon_text_size)),
                0, mExtraIconText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 设置字体
        extraIcon = new SpannableString(extraIcon);
        extraIcon.setSpan(new TypefaceSpan(InCallTypeface.getDefaultTypeface()),
                0, extraIcon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        extraIcon = new SpannableString(extraIcon);
        RadiusBackgroundSpan radiusBackspan = new RadiusBackgroundSpan();
        // 设置顶部margin
        radiusBackspan.setTopMargin(getExtraIconTopMargin());
        // 设置左边margin
        radiusBackspan.setLeftMargin(getExtraIconLeftMargin());
        // 设置上下左右padding
        radiusBackspan.setPadding(getDimenPx(R.dimen.mz_callcard_extra_icon_padding_horizontal),
                getDimenPx(R.dimen.mz_callcard_extra_icon_padding_vertical));
        // 设置圆角矩形radius
        radiusBackspan.setRadius(getDimenPx(R.dimen.mz_callcard_extra_icon_bg_rect_radius));
        extraIcon.setSpan(radiusBackspan, 0, extraIcon.length(),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return extraIcon;
    }

    protected int getSimIconTopMargin() {
        return getDimenPx(R.dimen.mz_caller_info_sim_icon_margin_top);
    }

    protected int getExtraIconTopMargin() {
        return getDimenPx(R.dimen.mz_callcard_extra_icon_margin_top);
    }

    protected int getExtraIconLeftMargin() {
        return getDimenPx(R.dimen.mz_callcard_extra_icon_margin_left);
    }

    private int getSimIconWidth() {
        if (mSimIndicator == null) {
            return 0;
        }
        return mSimIndicator.getIntrinsicWidth() + getExtraIconLeftMargin();
    }

    /** leftMargin + leftPading + textWidth + rightPadding*/
    private int getExtraIconWidth() {
        if (TextUtils.isEmpty(mExtraIconText)) {
            return 0;
        }
        int leftMargin = getExtraIconLeftMargin();
        int paddingHorizontal = getDimenPx(R.dimen.mz_callcard_extra_icon_padding_horizontal);
        Paint paint = new Paint();
        paint.setTextSize(getDimenPx(R.dimen.mz_callcard_extra_icon_text_size));
        return (int) (paint.measureText(mExtraIconText, 0, mExtraIconText.length()) + 2*paddingHorizontal + leftMargin);
    }

    /** 首行显示 名字+ExtraIcon+SimIcon, 若三者宽度相加超出最大宽度, 则对名字省略显示 */
    protected String getFirstLineDisplayText(String firstLineText) {
        return computeDisplayText(firstLineText, mFirstLineTextSize, getExtraIconWidth() + getSimIconWidth(),
                Typeface.create(InCallTypeface.getMediumTypeface(), Typeface.NORMAL));
    }

    /** 次行仅显示文字 若文字宽度超出最大宽度, 则对文字省略显示 */
    protected String getSecondaryDisplayText(String secondaryText) {
        return computeDisplayText(secondaryText, mSecondLineTextSize, 0,
                Typeface.create(InCallTypeface.getDefaultTypeface(), Typeface.NORMAL));
    }

    private String computeDisplayText(String originalText, int textSize, final int extraWidth, Typeface typeface) {
        String ret = originalText;
        // measure出来的textWidth比maxWidth只小几px时后面一个字和"..."会自动换行, why?
        // 因此maxWidth减掉一个padding
        final int maxWidth = mMaxTextContentWidth - getPaddingStart();
        if (maxWidth > 0) {
            Paint paint = new Paint();
            paint.setTextSize(textSize);
            paint.setTypeface(typeface);
            int textWidth = (int)paint.measureText(ret);
            while(!okToShowAsSingleline(textWidth, extraWidth, maxWidth)) {
                if (ret.length() > 2) {
                    // BlueSky -> BlueSk..., 逐个缩减并加上"..."看是否仍然超过最大长度
                    ret = ret.substring(0, ret.length() - 1);
                    ret = ret.trim(); // 移除最后的空格
                    String oldStr = ret;
                    String newStr = ret + "...";
                    int oldTextWidth = textWidth;
                    textWidth = (int)paint.measureText(newStr);
                    if (okToShowAsSingleline(textWidth, extraWidth, maxWidth)) {
                        ret = newStr;
                        if (DbgUtils.DBG_CIV) {
                            int strWidth = (int)paint.measureText(oldStr);
                            log("computeText() check!!!  get: " + ret
                                    + ", strWidth: " + strWidth
                                    + ", ...W: " + paint.measureText("...")
                                    + ", textWidth: " + textWidth
                                    + ", totalWidth: " + (textWidth + extraWidth));
                            log(" ");
                        }
                        break;
                    }
                    if (DbgUtils.DBG_CIV) {
                        log("computeText() toCheck...  text: " + ret
                                + ", textWidth: " + oldTextWidth + " -> " + textWidth
                                + ", extraWidth: " + extraWidth
                                + ", totalWidth: " + (textWidth + extraWidth)
                                + ", maxWidth: " + maxWidth + ", padding: " + getPaddingLeft());
                    }
                } else {
                    break;
                }
            }
        }
        return ret;
    }

    private boolean okToShowAsSingleline(int textWidth, int extraIconWidth, int maxWidth) {
        return textWidth + extraIconWidth < maxWidth;
    }

    protected int getDimenPx(int dimenId) {
        return getResources().getDimensionPixelSize(dimenId);
    }

    /** 设置一行显示内容的最大宽度, 若每一行要显示的内容超出此宽度, 则省略显示 */
    public void setMaxContentWidth(int width) {
        mMaxTextContentWidth = width;
    }

    /** 获取第一行文案 */
    protected String getFirstLineText() {
        String text;
        if (TextUtils.isEmpty(mName)) {
            text = mNumber;
        } else {
            text = mName;
        }

        return text;
    }

    /** 获取第二行文案 */
    protected String getSecondLineText() {
        // FLYME:zhulanting@Incallui: [#691642, 2018/1/15] {@
        if (!TextUtils.isEmpty(mVideoCallDialingStatus)) {
            return mVideoCallDialingStatus;
        }
        // @}
        StringBuilder sb = new StringBuilder();
        if (TextUtils.isEmpty(mName)) { // 姓名为空时，第二行不显示号码
            if (!TextUtils.isEmpty(mCallerLabel)) {
                sb.append(mCallerLabel);
            } else if (!TextUtils.isEmpty(mLocation)) {
                sb.append(mLocation);
            }
        } else {
            if (TextUtils.isEmpty(mNumber)) {
                if (!TextUtils.isEmpty(mCallerLabel)) {
                    sb.append(mCallerLabel);
                } else if (!TextUtils.isEmpty(mLocation)) {
                    sb.append(mLocation);
                }
            } else {
                sb.append(mNumber);
                if (!TextUtils.isEmpty(mCallerLabel)) {
                    sb.append("  ").append(mCallerLabel);
                } else if (!TextUtils.isEmpty(mLocation)) {
                    sb.append("  ").append(mLocation);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 高亮显示名字
     * @param flag
     */
    public void highLightName(boolean flag) {
        mHighLightName = flag;
    }

    /**
     * 对比2个字符串值是否发生变动
     * @param oldValue
     * @param newValue
     * @return
     */
    protected boolean isSame(String oldValue, String newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }

        if (oldValue == null || newValue == null) {
            return false;
        }

        if (oldValue.equals(newValue)) {
            return true;
        }

        return false;
    }

    private String getExtraIconText(Call call) {
        if (call != null && call.isCallbackCall()) {
            return getResources().getString(R.string.text_callback);
        } else if (call != null && call.isWebCall()) {
            return getResources().getString(R.string.web_call_tag);
        } else {
            return null;
        }
    }

    /** 清除View中显示的CallerInfo信息 */
    public void release() {
        setText(null);
        // Don't call set***() which will buildText()
        mName = null;
        mNumber = null;
        mLocation = null;
        mSlotId = -1;
        mSimIndicator = null;
        destroyDrawingCache();
    }

    protected void log(String msg) {
        if (DbgUtils.DBG_CIV) {
            Log.d(TAG, msg);
        }
    }
}
