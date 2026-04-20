package com.deadball.scene3d;

import com.deadball.core.Game;
import com.deadball.entities.Ball;
import com.deadball.entities.Goalkeeper;
import com.deadball.entities.Player;
import com.deadball.utils.GameConstants;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/**
 * JavaFX 3D pitch: camera behind the penalty spot looking at the goal.
 * Simulation still uses 2D world coords ({@code posX}, {@code posY}); this class maps them to 3D.
 */
public class GameScene3D {

    private static final double X_SCALE = 0.11;
    /** Camera looks along +Z; goal is farther (+Z), penalty nearer (−Z). */
    private static final double Z_AT_GOAL = 78;
    private static final double Z_AT_PENALTY = -42;
    /** Striker stands to the side so the ball at the spot stays visible. */
    private static final double STRIKER_OFFSET_X = -13.5;
    /** Nudge striker toward the goal (+Z); ball stays nearer the camera so it is drawn in front. */
    private static final double STRIKER_Z_AHEAD = 9.0;
    /** Toward camera (−Z) from the goal plane so the crosshair sits in front of the keeper mesh. */
    private static final double RETICLE_Z_IN_FRONT = 28.0;
    /** Ball resting height at the penalty spot (matches previous ball mesh offset). */
    private static final double BALL_REST_WORLD_Y = -1.15;

    private final Group root;
    private final Sphere ballMesh;
    private final Group keeperGroup;
    private final Group playerGroup;
    private final Group goalGroup;
    private final Group reticleGroup;
    private Box netBox;
    private PhongMaterial netMaterial;
    private PhongMaterial netFlashMaterial;
    private final Rotate keeperTilt = new Rotate(0, Rotate.Z_AXIS);

    public GameScene3D() {
        root = new Group();

        PhongMaterial grass = new PhongMaterial(Color.web("#1a6b24"));
        grass.setSpecularColor(Color.DARKGREEN);

        double pitchW = 260;
        double pitchD = 200;
        Box ground = new Box(pitchW, 2.5, pitchD);
        ground.setMaterial(grass);
        ground.setTranslateY(1.25);
        ground.setTranslateZ((Z_AT_GOAL + Z_AT_PENALTY) / 2.0);

        // Alternating mowing stripes. Thin boxes sit a hair above the pitch to avoid z-fighting.
        Group mowingStripes = new Group();
        PhongMaterial stripeMat = new PhongMaterial(Color.web("#2b8b34"));
        int stripes = 8;
        double stripeDepth = pitchD / stripes;
        for (int i = 0; i < stripes; i += 2) {
            Box stripe = new Box(pitchW, 0.05, stripeDepth - 0.2);
            stripe.setMaterial(stripeMat);
            stripe.setTranslateY(-0.02);
            stripe.setTranslateZ(Z_AT_PENALTY + stripeDepth * (i + 0.5));
            mowingStripes.getChildren().add(stripe);
        }

        Group skyTint = new Group();
        Box sky = new Box(400, 220, 2);
        PhongMaterial skyMat = new PhongMaterial(Color.web("#3a6ea8"));
        sky.setMaterial(skyMat);
        sky.setTranslateY(-115);
        sky.setTranslateZ(Z_AT_GOAL + 55);

        // Dark stand / crowd strip sandwiched between the sky and the pitch behind the goal.
        Box crowd = new Box(400, 40, 2);
        PhongMaterial crowdMat = new PhongMaterial(Color.web("#1b2736"));
        crowdMat.setSpecularColor(Color.web("#334458"));
        crowd.setMaterial(crowdMat);
        crowd.setTranslateY(-5);
        crowd.setTranslateZ(Z_AT_GOAL + 50);
        skyTint.getChildren().addAll(sky, crowd);

        goalGroup = buildGoal();
        goalGroup.setTranslateZ(Z_AT_GOAL);

        ballMesh = new Sphere(1.15);
        PhongMaterial ballMat = new PhongMaterial(Color.BLACK);
        ballMat.setSpecularColor(Color.GRAY);
        ballMesh.setMaterial(ballMat);

        keeperGroup = buildKeeperBody();
        keeperGroup.getTransforms().add(keeperTilt);
        playerGroup = buildStrikerBody();
        reticleGroup = buildAimReticle();
        reticleGroup.setDepthTest(DepthTest.DISABLE);

        AmbientLight ambient = new AmbientLight(Color.rgb(200, 200, 210, 0.55));
        PointLight sun = new PointLight(Color.rgb(255, 255, 245, 0.9));
        sun.setTranslateX(-80);
        sun.setTranslateY(-120);
        sun.setTranslateZ(20);

        // Reticle after keeper (not hidden by keeper depth) but before ball (shot stays visible).
        root.getChildren().addAll(skyTint, ground, mowingStripes, goalGroup, keeperGroup,
                reticleGroup, playerGroup, ballMesh, ambient, sun);
    }

    private Group buildAimReticle() {
        Group g = new Group();
        PhongMaterial m = new PhongMaterial(Color.web("#00FF66"));
        m.setSpecularColor(Color.WHITE);
        double thick = 0.6;
        double arm = 1.5;
        Box horizontal = new Box(arm * 2, thick, thick);
        horizontal.setMaterial(m);
        horizontal.setCullFace(CullFace.NONE);
        Box vertical = new Box(thick, arm * 2, thick);
        vertical.setMaterial(m);
        vertical.setCullFace(CullFace.NONE);
        g.getChildren().addAll(horizontal, vertical);
        return g;
    }

    private Group buildGoal() {
        Group g = new Group();
        Color woodGrey = Color.web("#aeaeae");
        PhongMaterial post = new PhongMaterial(woodGrey);
        post.setSpecularColor(Color.web("#8a8a8a"));

        double halfW = (GameConstants.GOAL_RIGHT - GameConstants.GOAL_LEFT) / 2.0 * X_SCALE;
        double postTh = 0.55;
        double postH = 14;
        double crossW = halfW * 2 + postTh * 2;
        /** Cross center Y = −postH; lower face of cross (toward opening) is at −postH + postTh/2. */
        double crossBottomY = -postH + postTh / 2.0;
        double mouthBottomY = 0.0;
        double netHeight = mouthBottomY - crossBottomY;
        double netCenterY = (crossBottomY + mouthBottomY) / 2.0;

        Box left = new Box(postTh, postH, postTh);
        left.setMaterial(post);
        left.setTranslateX(-halfW - postTh / 2);
        left.setTranslateY(-postH / 2);

        Box right = new Box(postTh, postH, postTh);
        right.setMaterial(post);
        right.setTranslateX(halfW + postTh / 2);
        right.setTranslateY(-postH / 2);

        Box cross = new Box(crossW, postTh, postTh);
        cross.setMaterial(post);
        cross.setTranslateY(-postH);

        netMaterial = new PhongMaterial(Color.rgb(215, 218, 228, 0.38));
        netFlashMaterial = new PhongMaterial(Color.rgb(255, 240, 80, 0.55));
        netBox = new Box(halfW * 2, netHeight, 3);
        netBox.setMaterial(netMaterial);
        netBox.setTranslateY(netCenterY);
        netBox.setTranslateZ(-1.8);

        g.getChildren().addAll(left, right, cross, netBox);
        return g;
    }

    private Group buildKeeperBody() {
        Group g = new Group();
        PhongMaterial kit = new PhongMaterial(Color.web("#E85D04"));
        PhongMaterial head = new PhongMaterial(Color.web("#C65A00"));

        Box torso = new Box(5.5, 9, 2.2);
        torso.setMaterial(kit);
        torso.setTranslateY(-5);

        Sphere headS = new Sphere(2.2);
        headS.setMaterial(head);
        headS.setTranslateY(-11);

        g.getChildren().addAll(torso, headS);
        return g;
    }

    private Group buildStrikerBody() {
        Group g = new Group();
        PhongMaterial kit = new PhongMaterial(Color.web("#1e3a8a"));
        PhongMaterial skin = new PhongMaterial(Color.web("#d4a574"));

        Box torso = new Box(5, 8, 2);
        torso.setMaterial(kit);
        torso.setTranslateY(-4.5);

        Sphere headS = new Sphere(2.0);
        headS.setMaterial(skin);
        headS.setTranslateY(-10);

        g.getChildren().addAll(torso, headS);
        return g;
    }

    private static double worldX(double posX) {
        return (posX - GameConstants.SCREEN_WIDTH / 2.0) * X_SCALE;
    }

    private static double worldZ(double posY) {
        double denom = GameConstants.PENALTY_SPOT_Y - GameConstants.GOAL_TOP;
        if (denom < 1e-6) {
            return Z_AT_GOAL;
        }
        double t = (posY - GameConstants.GOAL_TOP) / denom;
        return (1.0 - t) * Z_AT_GOAL + t * Z_AT_PENALTY;
    }

    /**
     * Depth for the ball only: from the penalty spot to the goal line we interpolate Z; once the
     * sim Y crosses into the goal frame ({@code <= GOAL_BOTTOM}), keep Z at the goal plane so the
     * ball does not appear to stop short of the net (linear GOAL_TOP→penalty mapping was wrong for
     * the mouth of the goal).
     */
    private static double worldZBall(double posY) {
        double penY = GameConstants.PENALTY_SPOT_Y;
        double mouthY = GameConstants.GOAL_BOTTOM;
        if (posY >= mouthY) {
            double span = penY - mouthY;
            if (span < 1e-6) {
                return Z_AT_GOAL;
            }
            double t = (posY - mouthY) / span;
            return (1.0 - t) * Z_AT_GOAL + t * Z_AT_PENALTY;
        }
        return Z_AT_GOAL;
    }

    private static void placeOnGround(Node n, double x, double z, double yOffset) {
        n.setTranslateX(x);
        n.setTranslateZ(z);
        n.setTranslateY(yOffset);
    }

    /**
     * Map sim Y (same space as aim/reticle) to JavaFX translateY on the goal plane.
     * Crossbar matches the goal mesh ({@code -postH}); mouth low matches post bottoms (~{@code 0}),
     * not positive Y (which reads as under the frame on the grass). {@code t} caps at {@code 1} so
     * sim Y cannot plot below the goal mouth.
     */
    private static double goalPlaneWorldY(double simY) {
        double gh = GameConstants.GOAL_BOTTOM - GameConstants.GOAL_TOP;
        if (gh < 1.0) {
            return -6.0;
        }
        double t = (simY - GameConstants.GOAL_TOP) / gh;
        t = Math.max(-1.45, Math.min(1.0, t));
        double yCrossbar = -14.0;
        double yMouthLow = -1.0; // to not let the reticle go under the goal frame
        return yCrossbar + t * (yMouthLow - yCrossbar);
    }

    /**
     * Places the ball on the straight 3D line from the penalty contact point to the reticle pose at
     * shot time. {@code t} comes from {@link Ball#getShotRayProgress()} so logic and display stay aligned.
     */
    private void placeBallAlongShotRay(Ball ball) {
        double sx = ball.getShotStartX();
        double sy = ball.getShotStartY();
        double ax = ball.getShotAimX();
        double ay = ball.getShotAimY();
        double t = Math.max(0.0, ball.getShotRayProgress());
        double swx = worldX(sx);
        double swz = worldZBall(sy);
        double swy = BALL_REST_WORLD_Y;
        double ewx = worldX(ax);
        double ewz = Z_AT_GOAL - RETICLE_Z_IN_FRONT;
        double ewy = goalPlaneWorldY(ay);
        double bx = swx + t * (ewx - swx);
        double by = swy + t * (ewy - swy);
        double bz = swz + t * (ewz - swz);
        placeOnGround(ballMesh, bx, bz, by);
    }

    public void sync(Game game) {
        String state = game.getGameState();
        boolean menu = GameConstants.STATE_MENU.equals(state);

        ballMesh.setVisible(!menu);
        playerGroup.setVisible(!menu);
        keeperGroup.setVisible(!menu);
        goalGroup.setVisible(!menu);

        if (menu) {
            reticleGroup.setVisible(false);
            return;
        }

        boolean aiming = GameConstants.STATE_PLAYING.equals(state) && "AIM".equals(game.getGamePhase());
        reticleGroup.setVisible(aiming);
        if (aiming) {
            double rx = worldX(game.getPlayer().getReticleX());
            reticleGroup.setTranslateX(rx);
            double aimY = Math.min(game.getPlayer().getReticleY(), GameConstants.GOAL_BOTTOM);
            reticleGroup.setTranslateY(goalPlaneWorldY(aimY));
            reticleGroup.setTranslateZ(Z_AT_GOAL - RETICLE_Z_IN_FRONT);
        }

        Ball ball = game.getBall();
        if (ball.hasShotRay()) {
            placeBallAlongShotRay(ball);
        } else {
            placeOnGround(ballMesh, worldX(ball.getPosX()), worldZBall(ball.getPosY()), BALL_REST_WORLD_Y);
        }

        Player player = game.getPlayer();
        double pz = worldZ(player.getPosY()) + STRIKER_Z_AHEAD;
        double px = worldX(player.getPosX()) + STRIKER_OFFSET_X;
        placeOnGround(playerGroup, px, pz, 0);

        Goalkeeper k = game.getGoalkeeper();
        double kx = k.getPosX();
        int dive = k.getCurrentDiveDirection();
        if (dive == -1) {
            kx -= GameConstants.KEEPER_DIVE_SHIFT_X;
        } else if (dive == 1) {
            kx += GameConstants.KEEPER_DIVE_SHIFT_X;
        }
        placeOnGround(keeperGroup, worldX(kx), worldZ(k.getPosY()), 0);
        // Tilt keeper torso toward dive side (positive Z rotation leans screen-right).
        keeperTilt.setAngle(dive * 35.0);

        netBox.setMaterial(game.getGoal().isNetFlashVisible() ? netFlashMaterial : netMaterial);
    }

    public Group getRoot() {
        return root;
    }

    /** Eye position; camera faces +Z (toward the goal). */
    public static Point3D cameraPosition() {
        return new Point3D(0, -30, -92);
    }
}
