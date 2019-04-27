/**
 * Copyright 2008 - 2019 The Loon Game Engine Authors
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
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.utils.html;

import javax.swing.text.html.StyleSheet;

import loon.LSystem;
import loon.canvas.LColor;
import loon.font.IFont;
import loon.opengl.GLEx;
import loon.utils.MathUtils;
import loon.utils.TArray;
import loon.utils.html.command.DisplayCommand;
import loon.utils.html.command.ImageCommand;
import loon.utils.html.command.LineCommand;
import loon.utils.html.command.TextCommand;
import loon.utils.html.css.CssColor;
import loon.utils.html.css.CssDimensions;
import loon.utils.html.css.CssDimensions.Rect;
import loon.utils.html.css.CssStyleBuilder;
import loon.utils.html.css.CssStyleNode;
import loon.utils.html.css.CssStyleSheet;
import loon.utils.html.css.CssValue;

/**
 * 构建中,逐渐实现w3c标准……
 */
public class HtmlDisplay {

	private TArray<DisplayCommand> displays;

	private float width, height;

	private TArray<CssStyleSheet> cssSheets;

	private CssDimensions cssBlock;

	private LColor defaultColor;

	private LColor backgroundColor;

	private String defaultFontName;

	public HtmlDisplay(float w, float h, LColor color) {
		this.cssBlock = CssDimensions.createDimension(w, h);
		this.cssSheets = new TArray<CssStyleSheet>();
		this.displays = new TArray<DisplayCommand>();
		this.width = w;
		this.height = h;
		this.backgroundColor = LColor.white;
		defaultColor = color;
	}

	public void parse(HtmlElement element) {

		TArray<HtmlElement> looper = element.childs();

		Rect lastRect = null;

		int sysSize = LSystem.getSystemGameFont().getHeight() + 5;

		float lineWidth = 0;
		float lineHeight = 0;

		boolean newLine = false;

		int newLineAmount = 0;

		while (looper.size > 0) {
			TArray<HtmlElement> next = new TArray<HtmlElement>();
			for (HtmlElement node : looper) {

				String tagName = node.getName();
				DisplayCommand display = null;

				CssStyleSheet cssSheet = node.getStyleSheet();

				if (cssSheet.size() > 0) {
					cssSheets.add(cssSheet);
				} else {
					cssSheet = cssSheets.last();
				}

				if (lastRect != null) {
					if (lineWidth - lastRect.width > width) {
						lineWidth = 0;
					}
				} else {
					if (lineWidth > width) {
						lineWidth = 0;
					}
				}

				if (newLine) {
					lineWidth = 0;
					if (lastRect != null) {
						lineHeight += lastRect.height;
					} else {
						lineHeight += sysSize;
					}
					lineHeight += newLineAmount;
					newLineAmount = 0;
					newLine = false;
				}

				if (node.isBody()) {

					if (cssSheet.size() > 0) {

						CssStyleBuilder builder = new CssStyleBuilder();
						CssStyleNode cssNode = builder.build(node, cssSheet);

						CssValue value = cssNode.getValueOf("background-color");
						if (value != null && value instanceof CssColor) {
							backgroundColor = ((CssColor) value).getLColor();
						} else if (value != null) {
							backgroundColor = LColor.findName(value.getValueString());
						}

						value = cssNode.getValueOf("color");

						if (value != null && value instanceof CssColor) {
							defaultColor = ((CssColor) value).getLColor();
						} else if (value != null) {
							defaultColor = LColor.findName(value.getValueString());
						}

						value = cssNode.getValueOf("font-family");

						if (value != null) {
							defaultFontName = value.getValueString();
						}
					}

				} else if (node.isH()) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
					newLine = true;
					newLineAmount = sysSize;
				} else if ("a".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("b".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("label".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("span".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("font".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("img".equals(tagName)) {
					display = new ImageCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
				} else if ("p".equals(tagName)) {
					display = new TextCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
					newLine = true;
					newLineAmount = sysSize;
				} else if ("br".equals(tagName)) {
					lineWidth = 0;
					if (lastRect != null) {
						lineHeight += lastRect.height;
					} else {
						lineHeight += sysSize;
					}
				} else if ("hr".equals(tagName)) {
					display = new LineCommand(cssSheet, width, height, defaultColor);
					display.parser(node);
					newLine = true;
					newLineAmount = sysSize + 5;
				}

				if (display != null) {

					float height = (lastRect == null) ? 0 : lastRect.height;

					lastRect = display.getRect();

					display.addX(lineWidth);
					display.addY(lineHeight);

					display.update();

					if (height != 0 && lineWidth != 0) {
						lastRect.height = MathUtils.max(lastRect.height, height);
					}

					lineWidth += lastRect.width;

					displays.add(display);
				}
				next.addAll(node.childs());
			}
			looper = next;
		}
	}

	public void paint(GLEx g, float x, float y) {
		g.fillRect(x, y, width, height, backgroundColor);
		for (int i = 0; i < displays.size; i++) {
			DisplayCommand command = displays.get(i);
			if (command != null) {
				command.paint(g, x, y);
			}
		}
	}

	public CssDimensions getCssBlock() {
		return cssBlock;
	}

	public LColor getDefaultColor() {
		return defaultColor.cpy();
	}

	public void setDefaultColor(LColor defaultColor) {
		this.defaultColor = defaultColor;
	}

	public TArray<CssStyleSheet> getCssSheets() {
		return cssSheets;
	}

	public LColor getBackgroundColor() {
		return backgroundColor.cpy();
	}

	public void setBackgroundColor(LColor b) {
		this.backgroundColor = b;
	}

	public String getDefaultFontName() {
		return defaultFontName;
	}

	public void setDefaultFontName(String f) {
		this.defaultFontName = f;
	}
}
