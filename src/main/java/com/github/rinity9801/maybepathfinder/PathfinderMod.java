package com.github.rinity9801.maybepathfinder;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod(modid = "maybepathfinder", name = "Maybe Pathfinder", version = "1.3")
public class PathfinderMod {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int NODE_SPACING = 1;
    private static final int NODE_HEIGHT_RANGE = 3;

    private static List<Node> currentPath = new ArrayList<>();
    private static int currentIndex = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new GotoCommand());
    }

    public static class GotoCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "goto";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/goto <x> <y> <z>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 3) {
                sender.addChatMessage(new ChatComponentText("Usage: /goto <x> <y> <z>"));
                return;
            }

            int tx, ty, tz;
            try {
                tx = parseInt(args[0]);
                ty = parseInt(args[1]);
                tz = parseInt(args[2]);
            } catch (NumberInvalidException e) {
                sender.addChatMessage(new ChatComponentText("Coordinates must be valid integers."));
                return;
            }

            Vec3 startVec = mc.thePlayer.getPositionVector();
            Vec3 goalVec = new Vec3(tx + 0.5, ty, tz + 0.5);
            Node start = new Node(startVec);
            Node goal = new Node(goalVec);

            int range = Math.max(20, (int) start.pos.distanceTo(goal.pos) + 5);
            sender.addChatMessage(new ChatComponentText("Generating nodes with range: " + range));
            Set<Node> nodes = generateNodes(start, goal, range);

            nodes.add(start);
            nodes.add(goal);

            Map<Node, List<Node>> graph = connectNodes(nodes);

            if (!graph.containsKey(start)) {
                sender.addChatMessage(new ChatComponentText("Start node not found in graph."));
                return;
            }

            if (!graph.containsKey(goal)) {
                sender.addChatMessage(new ChatComponentText("Goal node not found in graph."));
                return;
            }

            List<Node> path = aStar(start, goal, graph);

            if (path == null) {
                sender.addChatMessage(new ChatComponentText("No path found."));
            } else {
                currentPath = path;
                currentIndex = 0;
                sender.addChatMessage(new ChatComponentText("Path found with " + path.size() + " steps."));
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }

    private static Set<Node> generateNodes(Node start, Node goal, int range) {
        Set<Node> nodes = new HashSet<>();
        BlockPos center = start.toBlockPos();
        BlockPos goalPos = goal.toBlockPos();

        int minX = Math.min(center.getX(), goalPos.getX()) - range;
        int maxX = Math.max(center.getX(), goalPos.getX()) + range;
        int minZ = Math.min(center.getZ(), goalPos.getZ()) - range;
        int maxZ = Math.max(center.getZ(), goalPos.getZ()) + range;

        for (int x = minX; x <= maxX; x += NODE_SPACING) {
            for (int z = minZ; z <= maxZ; z += NODE_SPACING) {
                for (int y = -NODE_HEIGHT_RANGE; y <= NODE_HEIGHT_RANGE; y++) {
                    BlockPos pos = new BlockPos(x, center.getY() + y, z);
                    if (isWalkable(pos)) {
                        Node node = new Node(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
                        nodes.add(node);
                    }
                }
            }
        }

        return nodes;
    }

    private static boolean isWalkable(BlockPos pos) {
        return mc.theWorld.isAirBlock(pos) &&
                mc.theWorld.isAirBlock(pos.up()) &&
                mc.theWorld.getBlockState(pos.down()).getBlock().getMaterial().isSolid();
    }

    private static Map<Node, List<Node>> connectNodes(Set<Node> nodes) {
        Map<Node, List<Node>> graph = new HashMap<>();
        for (Node node : nodes) {
            List<Node> neighbors = new ArrayList<>();
            for (Vec3 offset : directions()) {
                for (int dy = -1; dy <= 1; dy++) {
                    Vec3 adjusted = offset.addVector(0, dy, 0);
                    Node neighbor = new Node(node.pos.add(adjusted));
                    if (!neighbor.equals(node) && nodes.contains(neighbor) &&
                            node.pos.distanceTo(neighbor.pos) <= 2.2) {
                        neighbors.add(neighbor);
                    }
                }
            }
            if (!neighbors.isEmpty()) {
                graph.put(node, neighbors);
            } else {
                graph.putIfAbsent(node, new ArrayList<>());
            }
        }
        return graph;
    }

    private static List<Vec3> directions() {
        return Arrays.asList(
                new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 0, -1),
                new Vec3(1, 0, 1), new Vec3(-1, 0, -1),
                new Vec3(1, 0, -1), new Vec3(-1, 0, 1)
        );
    }

    private static List<Node> aStar(Node start, Node goal, Map<Node, List<Node>> graph) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.pos.distanceTo(goal.pos)));
        Map<Node, Node> cameFrom = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.equals(goal)) {
                List<Node> path = new ArrayList<>();
                while (current != null) {
                    path.add(0, current);
                    current = cameFrom.get(current);
                }
                return path;
            }

            closed.add(current);
            for (Node neighbor : graph.getOrDefault(current, Collections.emptyList())) {
                if (closed.contains(neighbor)) continue;
                if (!open.contains(neighbor)) {
                    cameFrom.put(neighbor, current);
                    open.add(neighbor);
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (currentPath.isEmpty() || mc.thePlayer == null || mc.theWorld == null) return;

        if (currentIndex >= currentPath.size()) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.setSprinting(false);
            return;
        }

        Vec3 target = currentPath.get(currentIndex).pos;
        EntityPlayerSP player = mc.thePlayer;

        double dx = target.xCoord - player.posX;
        double dy = target.yCoord - player.posY;
        double dz = target.zCoord - player.posZ;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.5) {
            currentIndex++;
            return;
        }

        double speed = 0.3;
        double norm = Math.sqrt(dx * dx + dz * dz);
        double mx = (dx / norm) * speed;
        double mz = (dz / norm) * speed;

        player.motionX = mx;
        player.motionZ = mz;
        player.setSprinting(true);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float currentYaw = player.rotationYaw;
        float yawDiff = wrapAngleTo180(targetYaw - currentYaw);

        float maxTurn = 20f;
        float newYaw = currentYaw + Math.max(-maxTurn, Math.min(maxTurn, yawDiff));
        player.rotationYaw = newYaw;

        if (dy > 0.5 && player.onGround) {
            player.jump();
        }
    }

    private static float wrapAngleTo180(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (currentPath.size() < 2) return;

        EntityPlayerSP player = mc.thePlayer;
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (Node node : currentPath) {
            Vec3 pos = node.pos;
            wr.pos(pos.xCoord - px, pos.yCoord + 0.1 - py, pos.zCoord - pz).color(0f, 1f, 0f, 1f).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static class Node {
        public final Vec3 pos;

        public Node(Vec3 pos) {
            this.pos = new Vec3(
                    Math.floor(pos.xCoord) + 0.5,
                    Math.floor(pos.yCoord),
                    Math.floor(pos.zCoord) + 0.5
            );
        }

        public BlockPos toBlockPos() {
            return new BlockPos(pos);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n = (Node) o;
            return this.pos.xCoord == n.pos.xCoord &&
                    this.pos.yCoord == n.pos.yCoord &&
                    this.pos.zCoord == n.pos.zCoord;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos.xCoord, pos.yCoord, pos.zCoord);
        }
    }
}
