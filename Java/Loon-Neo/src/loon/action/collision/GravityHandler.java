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
package loon.action.collision;

import loon.LRelease;
import loon.LSystem;
import loon.action.ActionBind;
import loon.geom.RectBox;
import loon.geom.Vector2f;
import loon.utils.MathUtils;
import loon.utils.ObjectMap;
import loon.utils.TArray;
import loon.utils.cache.Pool;

/**
 * 简单的重力控制器,使用时需要绑定Gravity
 */
public class GravityHandler implements LRelease {

	public static interface GravityUpdate {
		public void action(Gravity g, float x, float y);
	}

	private final Pool<GravityResult> resultGravity = new Pool<GravityResult>() {

		@Override
		protected GravityResult newObject() {
			return new GravityResult();
		}

	};

	private CollisionWorld collisionWorld;

	protected CollisionFilter worldCollisionFilter;

	private ObjectMap<ActionBind, Gravity> gravityMap;

	private GravityUpdate listener;

	private boolean closed;

	private int width, height;

	private int bindWidth;

	private int bindHeight;

	private float bindX;

	private float bindY;

	private float velocityX, velocityY;

	boolean isBounded;

	boolean isListener;

	boolean isEnabled;

	RectBox rectLimit;

	Gravity[] lazyObjects;

	TArray<Gravity> objects;

	TArray<Gravity> pendingAdd;

	TArray<Gravity> pendingRemove;

	private final TArray<Gravity> collisionObjects = new TArray<Gravity>();

	public GravityHandler() {
		this(LSystem.viewSize.getWidth(), LSystem.viewSize.getHeight());
	}

	public GravityHandler(int w, int h) {
		this.setLimit(w, h);
		this.gravityMap = new ObjectMap<ActionBind, Gravity>();
		this.objects = new TArray<Gravity>(10);
		this.pendingAdd = new TArray<Gravity>(10);
		this.pendingRemove = new TArray<Gravity>(10);
		this.lazyObjects = new Gravity[] {};
		this.isEnabled = true;
	}

	public boolean isGravityRunning() {
		if (objects != null) {
			for (int i = 0; i < objects.size; i++) {
				Gravity g = objects.get(i);
				if (g != null && !g.enabled) {
					return true;
				}
			}
		}
		return false;
	}

	public GravityHandler setLimit(int w, int h) {
		if (w > 0 && h > 0) {
			setBounded(true);
		} else {
			return this;
		}
		this.width = w;
		this.height = h;
		if (rectLimit == null) {
			this.rectLimit = new RectBox(0, 0, w, h);
		} else {
			this.rectLimit.setBounds(0, 0, w, h);
		}
		return this;
	}

	public void update(long elapsedTime) {
		if (!isEnabled) {
			return;
		}
		commits();
		final float second = MathUtils.max(elapsedTime / 1000f, 0.01f);
		for (Gravity g : lazyObjects) {

			if (g.enabled && g.bind != null) {

				final float accelerationX = g.accelerationX;
				final float accelerationY = g.accelerationY;
				final float angularVelocity = g.angularVelocity;
				final float gravity = g.g;

				bindWidth = (int) g.bind.getWidth();
				bindHeight = (int) g.bind.getHeight();
				bindX = g.bind.getX();
				bindY = g.bind.getY();

				if (angularVelocity != 0) {

					final float rotate = g.bind.getRotation() + angularVelocity * second;
					int[] newObjectRect = MathUtils.getLimit(bindX, bindY, bindWidth, bindHeight, rotate);

					bindWidth = newObjectRect[2];
					bindHeight = newObjectRect[3];

					newObjectRect = null;

					g.bind.setRotation(rotate);
				}

				if (accelerationX != 0 || accelerationY != 0) {
					g.velocityX += accelerationX * second;
					g.velocityY += accelerationY * second;
				}

				velocityX = g.velocityX;
				velocityY = g.velocityY;
				if (velocityX != 0 || velocityY != 0) {

					velocityX = bindX + (velocityX * second);
					velocityY = bindY + (velocityY * second);

					if (gravity != 0 && g.velocityX != 0) {
						velocityX += g.gadd;
					}
					if (gravity != 0 && g.velocityY != 0) {
						velocityY += g.gadd;
					}
					if (gravity != 0) {
						g.gadd += gravity;
					}

					if (isBounded) {
						if (g.bounce != 0f) {
							final int limitWidth = width - bindWidth;
							final int limitHeight = height - bindHeight;
							final boolean chageWidth = bindX >= limitWidth;
							final boolean chageHeight = bindY >= limitHeight;
							if (chageWidth) {
								bindX -= g.bounce + gravity;
								if (g.bounce > 0) {
									g.bounce -= (g.bounce + second) + MathUtils.random(0f, 5f);
								} else if (g.bounce < 0) {
									g.bounce = 0;
									bindX = limitWidth;
									g.limitX = true;
								}
							}
							if (chageHeight) {
								bindY -= g.bounce + gravity;
								if (g.bounce > 0) {
									g.bounce -= (g.bounce + second) + MathUtils.random(0f, 5f);
								} else if (g.bounce < 0) {
									g.bounce = 0;
									bindY = limitHeight;
									g.limitY = true;
								}
							}
							if (chageWidth || chageHeight) {
								movePos(g.bind, bindX, bindY);
								if (isListener) {
									listener.action(g, bindX, bindY);
								}
								return;
							}
						}
						int limitWidth = width - bindWidth;
						int limitHeight = height - bindHeight;
						velocityX = limitValue(g, velocityX, limitWidth);
						velocityY = limitValue(g, velocityY, limitHeight);
					}
					movePos(g.bind, velocityX, velocityY);
					if (isListener) {
						listener.action(g, velocityX, velocityY);
					}
				}
			}
		}
	}

	private float limitValue(Gravity g, float value, float limit) {
		if (g.g < 0f) {
			if (value < 0f) {
				value = 0f;
				g.limitX = true;
			}
		}
		if (g.g > 0f) {
			if (limit < value) {
				value = limit;
				g.limitY = true;
			}
		}
		return value;
	}

	protected void commits() {
		boolean changes = false;
		final int additionCount = pendingAdd.size;
		if (additionCount > 0) {
			final Object[] additionsArray = pendingAdd.toArray();
			for (int i = 0; i < additionCount; i++) {
				Gravity o = (Gravity) additionsArray[i];
				objects.add(o);
			}
			pendingAdd.clear();
			changes = true;
		}
		final int removalCount = pendingRemove.size;
		if (removalCount > 0) {
			final Object[] removalsArray = pendingRemove.toArray();
			for (int i = 0; i < removalCount; i++) {
				Gravity o = (Gravity) removalsArray[i];
				objects.remove(o);
			}
			pendingRemove.clear();
			changes = true;
		}
		if (changes) {
			int size = objects.size;
			if (lazyObjects == null || lazyObjects.length != size) {
				lazyObjects = new Gravity[size];
			}
			for (int i = 0; i < size; i++) {
				lazyObjects[i] = objects.get(i);
			}
		}
	}

	public Gravity[] getObjects() {
		return lazyObjects;
	}

	public int getCount() {
		return lazyObjects.length;
	}

	public int getConcreteCount() {
		return lazyObjects.length + pendingAdd.size - pendingRemove.size;
	}

	public Gravity get(int index) {
		if (index > -1 && index < lazyObjects.length) {
			return lazyObjects[index];
		} else {
			return null;
		}
	}

	public Gravity add(ActionBind o, float vx, float vy) {
		return add(o, vx, vy, 0);
	}

	public Gravity add(ActionBind o, float vx, float vy, float ave) {
		return add(o, vx, vy, 0, 0, ave);
	}

	public Gravity add(ActionBind o, float vx, float vy, float ax, float ay, float ave) {
		Gravity g = gravityMap.get(o);
		if (g == null) {
			gravityMap.put(o, (g = new Gravity(o)));
		}
		g.velocityX = vx;
		g.velocityY = vy;
		g.accelerationX = ax;
		g.accelerationY = ay;
		g.angularVelocity = ave;
		add(g);
		return g;
	}

	public Gravity add(ActionBind o) {
		Gravity g = gravityMap.get(o);
		if (g == null) {
			gravityMap.put(o, (g = new Gravity(o)));
		}
		return add(g);
	}

	public Gravity get(ActionBind o) {
		return gravityMap.get(o);
	}

	public Gravity add(Gravity o) {
		if (!pendingAdd.contains(o)) {
			pendingAdd.add(o);
		}
		return o;
	}

	public Gravity remove(Gravity o) {
		pendingRemove.add(o);
		return o;
	}

	public void removeAll() {
		final int count = objects.size;
		for (int i = 0; i < count; i++) {
			pendingRemove.add(objects.get(i));
		}
		pendingAdd.clear();
	}

	public Gravity getObject(String name) {
		commits();
		for (Gravity o : lazyObjects) {
			if (o != null) {
				if (o.name != null) {
					if (o.name.equals(name)) {
						return o;
					}
				}
			}
		}
		return null;
	}

	public GravityResult getCollisionBetweenObjects(Gravity target) {
		return getCollisionBetweenObjects(target, objects, rectLimit, false);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, RectBox pathBounds, boolean clearVelocity) {
		return getCollisionBetweenObjects(target, objects, pathBounds, clearVelocity);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, boolean clearVelocity) {
		return getCollisionBetweenObjects(target, objects, rectLimit, 1f, 1f / 100f, clearVelocity);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, TArray<Gravity> otherObjects) {
		return getCollisionBetweenObjects(target, otherObjects, rectLimit);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, TArray<Gravity> otherObjects, RectBox pathBounds) {
		return getCollisionBetweenObjects(target, otherObjects, pathBounds, false);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, TArray<Gravity> otherObjects, RectBox pathBounds,
			boolean clearVelocity) {
		return getCollisionBetweenObjects(target, otherObjects, pathBounds, 1f, 1f / 100f, clearVelocity);
	}

	public GravityResult getCollisionBetweenObjects(Gravity target, TArray<Gravity> otherObjects, RectBox pathBounds,
			float scale, float deviation, boolean clearVelocity) {

		GravityResult result = resultGravity.obtain();

		float remainingVX = target.velocityX * scale;
		float remainingVY = target.velocityY * scale;
		float positionX = target.bounds.x;
		float positionY = target.bounds.y;
		float halfWidth = target.bounds.width * 0.5f;
		float halfHeight = target.bounds.height * 0.5f;
		float moveAmountX = 0;
		float moveAmountY = 0;
		boolean lastIteration = false;

		if (MathUtils.abs(remainingVX) >= MathUtils.abs(remainingVY)) {
			if (MathUtils.abs(remainingVX) > MathUtils.abs(halfWidth)) {
				moveAmountX = halfWidth * remainingVX;
			} else {
				moveAmountX = remainingVX;
			}
			if (MathUtils.abs(remainingVY) > 0) {
				moveAmountY = remainingVY * (remainingVX == 0 ? 1 : MathUtils.abs(remainingVY / remainingVX));
			}
		} else {
			if (MathUtils.abs(remainingVY) > MathUtils.abs(halfHeight)) {
				moveAmountY = halfHeight * remainingVY;
			} else {
				moveAmountY = remainingVY;
			}

			if (MathUtils.abs(remainingVX) > 0) {
				moveAmountX = remainingVX * (remainingVY == 0 ? 1 : MathUtils.abs(remainingVX / remainingVY));
			}
		}

		result.normal.setZero();

		collisionObjects.clear();

		for (Gravity b : otherObjects) {
			if (b.bounds.overlaps(pathBounds)) {
				collisionObjects.add(b);
			}
		}

		for (;;) {
			result.steps++;
			for (Gravity b : collisionObjects) {
				if (CollisionHelper.intersects(positionX, positionY, target.bounds.width, target.bounds.height,
						b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height, false)) {

					float overlapX = 0;
					float overlapY = 0;
					Vector2f normal = result.normal;
					normal.setZero();

					if (target.bounds.x <= b.bounds.x) {
						overlapX = (target.bounds.x + target.bounds.width) - b.bounds.x;
						normal.x = -1;
					} else {
						overlapX = (b.bounds.x + b.bounds.width) - target.bounds.x;
						normal.x = 1;
					}

					if (target.bounds.y <= b.bounds.y) {
						overlapY = (target.bounds.y + target.bounds.height) - b.bounds.y;
						normal.y = -1;
					} else {
						overlapY = (b.bounds.y + b.bounds.height) - target.bounds.y;
						normal.y = 1;
					}

					if (MathUtils.abs(overlapX) < MathUtils.abs(overlapY)) {
						normal.y = 0;
					}

					if (MathUtils.abs(overlapY) < MathUtils.abs(overlapX)) {
						normal.x = 0;
					}

					if (MathUtils.abs(overlapX) > b.bounds.width && MathUtils.abs(overlapY) > b.bounds.height) {
						continue;
					}

					if (normal.x == 1) {
						positionX = b.bounds.x + b.bounds.width;
						remainingVX = 0;
						if (clearVelocity) {
							target.velocityX = 0;
						}
						moveAmountX = 0;
					} else if (normal.x == -1) {
						positionX = b.bounds.x - target.bounds.width;
						remainingVX = 0;
						if (clearVelocity) {
							target.velocityX = 0;
						}
						moveAmountX = 0;
					}

					if (normal.y == 1) {
						positionY = b.bounds.y + b.bounds.height;
						remainingVY = 0;
						if (clearVelocity) {
							target.velocityY = 0;
						}
						moveAmountY = 0;
					} else if (normal.y == -1) {
						positionY = b.bounds.y - target.bounds.height;
						remainingVY = 0;
						if (clearVelocity) {
							target.velocityY = 0;
						}
						moveAmountY = 0;
					}

					result.isCollided = true;
				}

				if (positionY + target.bounds.height > b.bounds.y && positionY < b.bounds.y + b.bounds.height) {
					if ((positionX + target.bounds.width == b.bounds.x && remainingVX > 0)
							|| (positionX == b.bounds.x + b.bounds.width && remainingVX < 0)) {
						if ((positionX + target.bounds.width == b.bounds.x && remainingVX > 0)
								&& result.normal.x == 0) {
							result.normal.x = -1;
						} else if ((positionX == b.bounds.x + b.bounds.width && remainingVX < 0)
								&& result.normal.x == 0) {
							result.normal.x = 1;
						}
						remainingVX = 0;
						if (clearVelocity) {
							target.velocityX = 0;
						}
						moveAmountX = 0;
						result.isCollided = true;
					}
				}

				if (positionX + target.bounds.width > b.bounds.x && positionX < b.bounds.x + b.bounds.width) {
					if ((positionY + target.bounds.height == b.bounds.y && remainingVY > 0)
							|| (positionY == b.bounds.y + b.bounds.height && remainingVY < 0)) {

						if ((positionY + target.bounds.height == b.bounds.y && remainingVY > 0)
								&& result.normal.y == 0) {
							result.normal.y = -1;
						} else if ((positionY == b.bounds.y + b.bounds.height && remainingVY < 0)
								&& result.normal.y == 0) {
							result.normal.y = 1;
						}

						remainingVY = 0;
						if (clearVelocity) {
							target.velocityY = 0;
						}
						moveAmountY = 0;
						result.isCollided = true;
					}
				}

			}

			if (!lastIteration) {
				if (MathUtils.abs(remainingVX) < MathUtils.abs(moveAmountX)) {
					moveAmountX = remainingVX;
				}
				if (MathUtils.abs(remainingVY) < MathUtils.abs(moveAmountY)) {
					moveAmountY = remainingVY;
				}
				positionX += moveAmountX;
				positionY += moveAmountY;

				remainingVX -= moveAmountX;
				remainingVY -= moveAmountY;
			}

			if (!lastIteration && MathUtils.isEqual(0, remainingVX, deviation)
					&& MathUtils.isEqual(0, remainingVY, deviation)) {
				lastIteration = true;
				remainingVX = 0;
				remainingVY = 0;
			} else if (lastIteration) {
				break;
			}
		}
		result.position.set(positionX, positionY);
		collisionObjects.clear();
		return result;
	}

	public GravityHandler movePos(ActionBind bind, float x, float y) {
		return movePos(bind, x, y, -1f, -1f);
	}

	public GravityHandler movePos(ActionBind bind, float x, float y, float lastX, float lastY) {
		if (bind == null) {
			return this;
		}
		if (collisionWorld != null) {
			if (worldCollisionFilter == null) {
				worldCollisionFilter = CollisionFilter.getDefault();
			}
			CollisionResult.Result result = collisionWorld.move(bind, x, y, worldCollisionFilter);
			if (lastX != -1 && lastY != -1) {
				if (result.goalX != x || result.goalY != y) {
					bind.setLocation(lastX, lastY);
				} else {
					bind.setLocation(result.goalX, result.goalY);
				}
			} else {
				bind.setLocation(result.goalX, result.goalY);
			}
		} else {
			bind.setLocation(x, y);
		}
		return this;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isBounded() {
		return isBounded;
	}

	public void setBounded(boolean isBounded) {
		this.isBounded = isBounded;
	}

	public boolean isListener() {
		return isListener;
	}

	public GravityHandler setListener(GravityUpdate listener) {
		return onUpdate(listener);
	}

	public GravityHandler onUpdate(GravityUpdate listener) {
		this.listener = listener;
		this.isListener = listener != null;
		return this;
	}

	public CollisionFilter getCollisionFilter() {
		return worldCollisionFilter;
	}

	public void setCollisionFilter(CollisionFilter filter) {
		this.worldCollisionFilter = filter;
	}

	public CollisionWorld getCollisionWorld() {
		return collisionWorld;
	}

	public void setCollisionWorld(CollisionWorld world) {
		this.collisionWorld = world;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		this.isEnabled = false;
		if (objects != null) {
			objects.clear();
			objects = null;
		}
		if (pendingAdd != null) {
			pendingAdd.clear();
			pendingAdd = null;
		}
		if (pendingAdd != null) {
			pendingAdd.clear();
			pendingAdd = null;
		}
		if (gravityMap != null) {
			gravityMap.clear();
			gravityMap = null;
		}
		if (lazyObjects != null) {
			for (Gravity g : lazyObjects) {
				if (g != null) {
					g.dispose();
					g = null;
				}
			}
		}
		closed = true;
	}

}
