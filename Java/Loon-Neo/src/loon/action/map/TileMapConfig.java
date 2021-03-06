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
package loon.action.map;

import java.util.StringTokenizer;

import loon.BaseIO;
import loon.LSystem;
import loon.utils.CollectionUtils;
import loon.utils.MathUtils;
import loon.utils.StringUtils;
import loon.utils.TArray;

public class TileMapConfig {

	private int[][] backMap;

	public int[][] getBackMap() {
		return backMap;
	}

	public void setBackMap(int[][] backMap) {
		this.backMap = backMap;
	}

	public static Field2D loadCharsField(String resName, int tileWidth, int tileHeight) {
		Field2D field = new Field2D(loadCharsMap(resName), tileWidth, tileHeight);
		return field;
	}

	public static int[][] loadCharsMap(String resName) {
		int[][] map = null;
		String result = BaseIO.loadText(resName);
		if (result == null) {
			return map;
		}
		StringTokenizer br = new StringTokenizer(result, LSystem.NL);
		String line = br.nextToken();
		int width = Integer.parseInt(line);
		line = br.nextToken();
		int height = Integer.parseInt(line);
		map = new int[width][height];
		for (int i = 0; i < width; i++) {
			line = br.nextToken();
			for (int j = 0; j < height; j++) {
				map[i][j] = line.charAt(j);
			}
		}
		return map;
	}

	public static TArray<int[]> loadList(final String fileName) {
		String result = BaseIO.loadText(fileName);
		if (result == null) {
			return null;
		}
		StringTokenizer br = new StringTokenizer(result, LSystem.NL);
		TArray<int[]> records = new TArray<int[]>(CollectionUtils.INITIAL_CAPACITY);
		for (; br.hasMoreTokens();) {
			result = StringUtils.replace(br.nextToken().trim(), LSystem.LS, LSystem.EMPTY);
			if (!StringUtils.isEmpty(result)) {
				String[] stringArray = result.split(",");
				int size = stringArray.length;
				int[] intArray = new int[size];
				for (int i = 0; i < size; i++) {
					intArray[i] = Integer.parseInt(stringArray[i]);
				}
				records.add(intArray);
			}
		}
		return records;
	}

	public static int[][] reversalXandY(final int[][] array) {
		int col = array[0].length;
		int row = array.length;
		int[][] result = new int[col][row];
		for (int y = 0; y < col; y++) {
			for (int x = 0; x < row; x++) {
				result[y][x] = array[x][y];
			}
		}
		return result;
	}

	public static int[][] loadAthwartArray(final String fileName) {
		TArray<int[]> list = loadList(fileName);
		int col = list.size;
		int[][] result = new int[col][];
		for (int i = 0; i < col; i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	public static int[][] loadJustArray(final String fileName) {
		TArray<int[]> list = loadList(fileName);
		int col = list.size;
		int[][] mapArray = new int[col][];
		for (int i = 0; i < col; i++) {
			mapArray[i] = list.get(i);
		}
		int row = ((mapArray[col > 0 ? col - 1 : 0]).length);
		int[][] result = new int[row][col];
		for (int y = 0; y < col; y++) {
			for (int x = 0; x < row; x++) {
				result[x][y] = mapArray[y][x];
			}
		}
		return result;
	}

	public static int[][] stringToIntArrays(String srcStr) {
		int[][] resArr = null;
		if ((srcStr == null) || (srcStr.length() == 0)) {
			return resArr;
		}
		try {
			String[] strLns = StringUtils.split(srcStr, '\n');
			resArr = new int[strLns.length][];
			for (int i = 0; i < strLns.length; i++) {
				String[] strPrms = StringUtils.split(strLns[i], ',');
				resArr[i] = new int[strPrms.length];
				for (int j = 0; j < strPrms.length; j++) {
					resArr[i][j] = stingToInt(strPrms[j]);
				}
			}
		} catch (Throwable ex) {
			LSystem.error("TileMapConfig stringToIntArrays exception", ex);
		}
		return resArr;
	}

	private static int stingToInt(String srcStr) {
		int resNo = 0;
		if (MathUtils.isNan(srcStr)) {
			try {
				resNo = Integer.parseInt(srcStr);
			} catch (Throwable ex) {
				LSystem.error("TileMapConfig stringToInt exception", ex);
			}
		}
		return resNo;
	}

	public static String[][] stringToStringArrays(String srcStr) {
		String[][] resArr = (String[][]) null;
		if ((srcStr == null) || (srcStr.length() == 0))
			return resArr;
		try {
			String[] strLns = StringUtils.split(srcStr, '\n');
			resArr = new String[strLns.length][];
			for (int i = 0; i < strLns.length; i++) {
				String[] strPrms = StringUtils.split(strLns[i], ',');
				resArr[i] = new String[strPrms.length];
				for (int j = 0; j < strPrms.length; j++) {
					resArr[i][j] = strPrms[j];
				}
			}
		} catch (Throwable ex) {
		}
		return resArr;
	}

}
