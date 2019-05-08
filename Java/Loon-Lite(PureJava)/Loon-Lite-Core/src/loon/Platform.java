/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
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
package loon;

import loon.event.KeyMake;
import loon.event.SysInput;

public interface Platform {

	public static enum Orientation {
		Portrait, Landscape;
	}

	public abstract void close();

	public abstract int getContainerWidth();

	public abstract int getContainerHeight();

	public abstract Orientation getOrientation();

	public abstract LGame getGame();

	public void sysText(SysInput.TextEvent event, KeyMake.TextType textType,
			String label, String initialValue);

	public void sysDialog(SysInput.ClickEvent event, String title, String text,
			String ok, String cancel);
}
