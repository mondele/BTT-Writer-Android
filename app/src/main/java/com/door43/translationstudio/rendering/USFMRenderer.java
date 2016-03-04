package com.door43.translationstudio.rendering;

import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.StyleSpan;

import com.door43.translationstudio.spannables.USXChar;
import com.door43.translationstudio.spannables.USFMNoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.USFMVersePinSpan;
import com.door43.translationstudio.spannables.USFMVerseSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the USFM rendering engine. This handles all of the rendering for USFM formatted source and translation
 * NOTE: when rendering large chunks of text it is important to always keep things as a CharSequence and not string
 * so that spans generated by prior rendering methods are not lost.
 */
public class USFMRenderer extends ClickableRenderingEngine {

    private Span.OnClickListener mNoteListener;
    private Span.OnClickListener mVerseListener;
    private boolean mRenderVerses = true;
    private int[] mExpectedVerseRange = new int[0];
    private boolean mSuppressLeadingMajorSectionHeadings = false;

    /**
     * Creates a new USFM rendering engine without any listeners
     */
    public USFMRenderer() {

    }

    /**
     * Creates a new USFM rendering engine with some custom click listeners
     * @param verseListener
     */
    public USFMRenderer(Span.OnClickListener verseListener, Span.OnClickListener noteListener) {
        mVerseListener = verseListener;
        mNoteListener = noteListener;
    }

    /**
     * if set to false verses will not be displayed in the output.
     *
     * @param enable default is true
     */
    public void setVersesEnabled(boolean enable) {
        mRenderVerses = enable;
    }

    /**
     * Specifies an inclusive range of verses expected in the input.
     * If a verse is not found it will be inserted at the front of the input.
     * @param verseRange
     */
    public void setPopulateVerseMarkers(int[] verseRange) {
        mExpectedVerseRange = verseRange;
    }

    /**
     * Set whether to suppress display of major section headers.
     *
     * <p>The intent behind this is that major section headers prior to chapter markers will be
     * displayed above chapter markers, but only in read mode.</p>
     *
     * @param suppressLeadingMajorSectionHeadings The value to set
     */
    public void setSuppressLeadingMajorSectionHeadings(boolean suppressLeadingMajorSectionHeadings) {
        mSuppressLeadingMajorSectionHeadings = suppressLeadingMajorSectionHeadings;
    }

    /**
     * Renders the USFM input into a readable form
     * @param in the raw input string
     * @return
     */
    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        out = trimWhitespace(out);
        out = renderLineBreaks(out);
        // TODO: this will strip out new lines. Eventually we may want to convert these to paragraphs.
        out = renderWhiteSpace(out);
        out = renderMajorSectionHeading(out);
        out = renderSectionHeading(out);
        out = renderParagraph(out);
        out = renderBlankLine(out);
        out = renderPoeticLine(out);
        out = renderRightAlignedPoeticLine(out);
        out = renderVerse(out);
        out = renderNote(out);
        out = renderChapterLabel(out);
        out = renderSelah(out);

        return out;
    }

    /**
     * Renders all the Selah tags
     * @param in
     * @return
     */
    private CharSequence renderSelah(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = USXChar.getPattern(USXChar.STYLE_SELAH);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n", span);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence trimWhitespace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(^\\s*|\\s*$)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders section headings.
     * @param in
     * @return
     */
    public CharSequence renderSectionHeading(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("s");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span, "\n");
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
    /**
     * Renders major section headings.
     * @param in
     * @return
     */
    public CharSequence renderMajorSectionHeading(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("ms");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;

            if (mSuppressLeadingMajorSectionHeadings && 0 == matcher.start()) {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
            } else {
                SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1).toUpperCase());
                span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span, "\n");
            }
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Strips out extra whitespace from the text
     * @param in
     * @return
     */
    public CharSequence renderWhiteSpace(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Strips out new lines and replaces them with a single space
     * @param in
     * @return
     */
    public CharSequence renderLineBreaks(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("(\\s*\\n+\\s*)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), " ");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all note tags
     * @param in
     * @return
     */
    public CharSequence renderNote(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile(USFMNoteSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            USFMNoteSpan note = USFMNoteSpan.parseNote(matcher.group(1),matcher.group(2));
            if(note != null) {
                note.setOnClickListener(mNoteListener);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), note.toCharSequence());
            } else {
                // failed to parse the note
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }

            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all verse tags
     * @param in
     * @return
     */
    public CharSequence renderVerse(CharSequence in) {
        CharSequence out = "";

        CharSequence insert = "";
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            insert = "\n"; // this is a hack to get around bug in JellyBean in rendering multiple
            // verses on a long line.  This hack messes up the paragraph formatting,
            // but at least JellyBean becomes usable and doesn't crash.
        }

        Pattern pattern = Pattern.compile(USFMVerseSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        List<Integer> foundVerses = new ArrayList<>();
        while(matcher.find()) {
            if(isStopped()) return in;
            if(mRenderVerses) {
                Span verse;
                if(mVerseListener == null) {
                    verse = new USFMVerseSpan(matcher.group(1));
                } else {
                    verse = new USFMVersePinSpan(matcher.group(1));
                }

                if (verse != null) {
                    // record found verses
                    int startVerse = ((USFMVerseSpan)verse).getStartVerseNumber();
                    int endVerse = ((USFMVerseSpan)verse).getEndVerseNumber();
                    boolean alreadyRendered = false;
                    if(endVerse > startVerse) {
                        // range of verses
                        for(int i = startVerse; i <= endVerse; i ++) {
                            if(!foundVerses.contains(i)) {
                                foundVerses.add(i);
                            } else {
                                alreadyRendered = true;
                            }
                        }
                    } else {
                        if(!foundVerses.contains(startVerse)) {
                            foundVerses.add(startVerse);
                        } else {
                            alreadyRendered = true;
                        }
                    }
                    // render verses not already found
                    if(!alreadyRendered) {
                        // exclude verses not within the range
                        boolean invalidVerse = false;
                        if(mExpectedVerseRange.length > 0) {
                            int minVerse = mExpectedVerseRange[0];
                            int maxVerse = (mExpectedVerseRange.length > 1) ? mExpectedVerseRange[1] : 0;
                            if(maxVerse == 0) maxVerse = minVerse;

                            int verseNumStart = ((USFMVerseSpan) verse).getStartVerseNumber();
                            int verseNumEnd = ((USFMVerseSpan) verse).getEndVerseNumber();
                            if(verseNumEnd == 0) verseNumEnd = verseNumStart;
                            invalidVerse = verseNumStart < minVerse || verseNumStart > maxVerse || verseNumEnd < minVerse || verseNumEnd > maxVerse;
                        }
                        if(!invalidVerse) {
                            verse.setOnClickListener(mVerseListener);
                            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), insert, verse.toCharSequence());
                        } else {
                            // exclude invalid verse
                            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                        }
                    } else {
                        // exclude duplicate verse
                        out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
                    }
                } else {
                    // failed to parse the verse
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
                }
            } else {
                // exclude verse from display
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));

        if(mRenderVerses) {
            // populate missing verses
            if (mExpectedVerseRange.length == 1) {
                if (!foundVerses.contains(mExpectedVerseRange[0])) {
                    // generate missing verse
                    Span verse;
                    if (mVerseListener == null) {
                        verse = new USFMVerseSpan(mExpectedVerseRange[0]);
                    } else {
                        verse = new USFMVersePinSpan(mExpectedVerseRange[0]);
                    }
                    verse.setOnClickListener(mVerseListener);
                    out = TextUtils.concat(verse.toCharSequence(), out);
                }
            } else if (mExpectedVerseRange.length == 2) {
                for (int i = mExpectedVerseRange[1]; i >= mExpectedVerseRange[0]; i--) {
                    if (!foundVerses.contains(i)) {
                        // generate missing verse
                        Span verse;
                        if (mVerseListener == null) {
                            verse = new USFMVerseSpan(i);
                        } else {
                            verse = new USFMVersePinSpan(i);
                        }
                        verse.setOnClickListener(mVerseListener);
                        out = TextUtils.concat(verse.toCharSequence(), out);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Renders all paragraph tags
     * @param in
     * @return
     */
    public CharSequence renderParagraph(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("p");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            String lineBreak = "";
            if(matcher.start() > 0) {
                lineBreak = "\n";
            }
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), lineBreak, "    ", in.subSequence(matcher.start(1), matcher.end(1)), "\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all blank line tags
     * @param in
     * @return
     */
    public CharSequence renderBlankLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraShortPattern("b");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n\n");
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders a chapter label
     * @param in
     * @return
     */
    public CharSequence renderChapterLabel(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("cl");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while (matcher.find()) {
            if(isStopped()) return in;

            SpannableString span = new SpannableString(in.subSequence(matcher.start(1), matcher.end(1)));
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out,  in.subSequence(lastIndex, matcher.start()), span);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderPoeticLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("q(\\d+)");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            int level = Integer.parseInt(matcher.group(1));
            SpannableString span = new SpannableString(in.subSequence(matcher.start(2), matcher.end(2)));
            span.setSpan(new StyleSpan(Typeface.NORMAL), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            String padding = "";
            for(int i = 0; i < level; i ++) {
                padding += "    ";
            }

            // outdent for verse markers
            if (level > 0 && span.toString().indexOf("<verse number") == 0) {
                padding = padding.substring(0, padding.length() - 2);
            }

            // don't stack new lines
            String leadingLineBreak = "";
            String trailingLineBreak = "";

            // leading
            if(in.subSequence(0, matcher.start()) != null) {
                String previous = in.subSequence(0, matcher.start()).toString().replace(" ", "");
                int lastLineBreak = previous.lastIndexOf("\n");
                if (lastLineBreak < previous.length() - 1) {
                    leadingLineBreak = "\n";
                }
            }

            // trailing
            if(in.subSequence(matcher.end(), in.length()) != null) {
                String next = in.subSequence(matcher.end(), in.length()).toString().replace(" ", "");
                int nextLineBreak = next.indexOf("\n");
                int nextParagraph = next.indexOf("<para");
                if (nextLineBreak > 0 && nextParagraph > 0) {
                    trailingLineBreak = "\n";
                }
            }

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), leadingLineBreak, padding, span, trailingLineBreak);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Renders all right-aligned poetic line tags
     * @param in
     * @return
     */
    public CharSequence renderRightAlignedPoeticLine(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = paraPattern("qr");
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;

        while(matcher.find()) {
            if(isStopped()) return in;
            SpannableStringBuilder span = new SpannableStringBuilder(matcher.group(1));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), "\n", span);
            lastIndex = matcher.end();
        }

        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Return the leading section heading, if any. Non-leading major section headings, and leading
     * headings of other types, are not included.
     *
     * <p>As this is a static helper method, behavior is unaffected by the value of
     * {@link mSuppressLeadingMajorSectionHeadings}.</p>
     *
     * @see http://ubs-icap.org/chm/usfm/2.4/paragraphs.htm
     * @param in The string to examine for a leading major section heading.
     * @return The leading major section heading; or the empty string if there is none.
     */
    public static CharSequence getLeadingMajorSectionHeading(CharSequence in) {
        Pattern pattern = paraPattern("ms");
        Matcher matcher = pattern.matcher(in);

        if(matcher.find() && 0 == matcher.start()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    /**
     * Returns a pattern that matches a para tag pair e.g. <para style=""></para>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*>\\s*(((?!</para>).)*)</para>", Pattern.DOTALL);  // TODO: 3/1/16 need to upgrade to USFM
    }

    /**
     * Returns a pattern that matches a single para tag e.g. <para style=""/>
     * @param style a string or regular expression to identify the style
     * @return
     */
    private static Pattern paraShortPattern(String style) {
        return Pattern.compile("<para\\s+style=\""+style+"\"\\s*/>", Pattern.DOTALL); // TODO: 3/1/16 need to upgrade to USFM
    }
}

