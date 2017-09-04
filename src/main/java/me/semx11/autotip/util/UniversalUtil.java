package me.semx11.autotip.util;

import static me.semx11.autotip.util.ReflectionUtil.findClazz;
import static me.semx11.autotip.util.ReflectionUtil.findField;
import static me.semx11.autotip.util.ReflectionUtil.findMethod;
import static me.semx11.autotip.util.ReflectionUtil.getConstructor;
import static me.semx11.autotip.util.ReflectionUtil.getEnum;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import me.semx11.autotip.Autotip;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

public class UniversalUtil {

    private static Class<?> componentClass;
    private static Class<?> textComponentClass;

    private static Method addChatMethod;

    private static Class<?> chatStyleClass;

    private static Class<?> clickEventClass;
    private static Class<?> clickEventActionClass;

    private static Class<?> hoverEventClass;
    private static Class<?> hoverEventActionClass;

    public static MinecraftVersion getMinecraftVersion() {
        try {
            Field f = findField(ForgeVersion.class, "mcVersion");
            if (f != null) {
                return MinecraftVersion.fromString((String) f.get(null));
            } else {
                return MinecraftVersion.V1_8;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return MinecraftVersion.V1_8;
        }
    }

    public static SocketAddress getRemoteAddress(ClientConnectedToServerEvent event) {
        SocketAddress address = null;
        try {
            Object networkManager = isLegacy()
                    ? findField(FMLNetworkEvent.class, "manager").get(event)
                    : findMethod(FMLNetworkEvent.class, new String[]{"getManager"})
                            .invoke(event);

            address = (SocketAddress) findMethod(
                    networkManager.getClass(),
                    new String[]{"func_74430_c", "getRemoteAddress"}
            ).invoke(networkManager);

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return address;
    }

    public static String getUnformattedText(ClientChatReceivedEvent event) {
        try {
            Object component = isLegacy()
                    ? findField(ClientChatReceivedEvent.class, "message").get(event)
                    : findMethod(ClientChatReceivedEvent.class, new String[]{"getMessage"})
                            .invoke(event);

            return getUnformattedText(component);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getUnformattedText(Object component) {
        try {
            return (String) findMethod(
                    componentClass,
                    new String[]{"func_150260_c", "getUnformattedText"}
            ).invoke(component);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getFormattedText(Object component) {
        try {
            return (String) findMethod(
                    componentClass,
                    new String[]{"func_150254_d", "getFormattedText"}
            ).invoke(component);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Entity getEntity(EntityJoinWorldEvent event) {
        try {
            Object entity = isLegacy()
                    ? findField(EntityEvent.class, "entity").get(event)
                    : findMethod(EntityEvent.class, new String[]{"getEntity"}).invoke(event);
            return (Entity) entity;
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void addChatMessage(String text) {
        addChatMessage(newComponent(text));
    }

    public static void addChatMessage(String text, String url, String hoverText) {
        addChatMessage(newComponent(text, url, hoverText));
    }

    private static void addChatMessage(Object component) {
        EntityPlayerSP thePlayer = Autotip.MC.thePlayer;
        try {
            addChatMethod.invoke(thePlayer, component);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Object newComponent(String text) {
        try {
            return getConstructor(textComponentClass, String.class).newInstance(text);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object newComponent(String text, String url, String hoverText) {
        try {
            Object chatStyle = chatStyleClass.newInstance();
            Object clickEvent;
            Object hoverEvent;

            if (url != null && !url.equals("")) {
                clickEvent = getConstructor(
                        clickEventClass,
                        clickEventActionClass,
                        String.class
                ).newInstance(getEnum(clickEventActionClass, "OPEN_URL"), url);

                findMethod(
                        chatStyleClass,
                        new String[]{"func_150241_a", "setChatClickEvent"}, // 1.8 - 1.12.1
                        clickEventClass
                ).invoke(chatStyle, clickEvent);
            }

            if (hoverText != null && !hoverText.equals("")) {
                hoverEvent = getConstructor(
                        hoverEventClass,
                        hoverEventActionClass,
                        componentClass
                ).newInstance(getEnum(hoverEventActionClass, "SHOW_TEXT"), newComponent(hoverText));

                findMethod(
                        chatStyleClass,
                        new String[]{"func_150209_a", "setChatHoverEvent"}, // 1.8 - 1.12.1
                        hoverEventClass
                ).invoke(chatStyle, hoverEvent);
            }

            Object chatComponent = newComponent(text);

            return findMethod(
                    textComponentClass,
                    new String[]{"func_150255_a", "setChatStyle"}, // 1.8 - 1.12.1
                    chatStyleClass
            ).invoke(chatComponent, chatStyle);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean isLegacy() {
        switch (Autotip.MC_VERSION) {
            case V1_8:
            case V1_8_8:
            case V1_8_9:
                return true;
            case V1_9:
            case V1_9_4:
            case V1_10:
            case V1_10_2:
            case V1_11:
            case V1_11_2:
            case V1_12:
            case V1_12_1:
                return false;
            default:
                return false;
        }
    }

    static {
        componentClass = findClazz(
                "net.minecraft.util.IChatComponent", // 1.8 - 1.8.9
                "net.minecraft.util.text.ITextComponent" // 1.9 - 1.12.1
        );
        textComponentClass = findClazz(
                "net.minecraft.util.ChatComponentText", // 1.8 - 1.8.9
                "net.minecraft.util.text.TextComponentString" // 1.9 - 1.12.1
        );
        addChatMethod = findMethod(
                EntityPlayerSP.class,
                new String[]{
                        "func_145747_a", // 1.8  - 1.8.9  | 1.11 - 1.12.1
                        "func_146105_b", // 1.9  - 1.10.2
                        "addChatMessage", // 1.8  - 1.8.9  | 1.11 - 1.12.1
                        "addChatComponentMessage", // 1.9  - 1.10.2
                        "sendMessage" // 1.11 - 1.12.1
                },
                componentClass
        );
        chatStyleClass = findClazz(
                "net.minecraft.util.ChatStyle", // 1.8 - 1.8.9
                "net.minecraft.util.text.Style" // 1.9 - 1.12.1
        );
        clickEventClass = findClazz(
                "net.minecraft.event.ClickEvent", // 1.8 - 1.8.9
                "net.minecraft.util.text.event.ClickEvent" // 1.9 - 1.12.1
        );
        clickEventActionClass = findClazz(
                "net.minecraft.event.ClickEvent$Action", // 1.8 - 1.8.9
                "net.minecraft.util.text.event.ClickEvent$Action" // 1.9 - 1.12.1
        );
        hoverEventClass = findClazz(
                "net.minecraft.event.HoverEvent", // 1.8 - 1.8.9
                "net.minecraft.util.text.event.HoverEvent" // 1.9 - 1.12.1
        );
        hoverEventActionClass = findClazz(
                "net.minecraft.event.HoverEvent$Action", // 1.8 - 1.8.9
                "net.minecraft.util.text.event.HoverEvent$Action" // 1.9 - 1.12.1
        );
    }

}
