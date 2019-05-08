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
package loon.canvas;

import loon.geom.Region;
import loon.utils.TArray;

public class NinePatchRegion {

	private TArray<Region> fixedRegions;

	private TArray<Region> patchRegions;

	public NinePatchRegion(TArray<Region> f, TArray<Region> p) {
		this.fixedRegions = f;
		this.patchRegions = p;
	}

	public TArray<Region> getFixedRegions() {
		return fixedRegions;
	}

	public TArray<Region> getPatchRegions() {
		return patchRegions;
	}

	@Override
	public String toString() {
		return "[fixRegions=" + fixedRegions + ", patchRegions=" + patchRegions + "]";
	}
}
