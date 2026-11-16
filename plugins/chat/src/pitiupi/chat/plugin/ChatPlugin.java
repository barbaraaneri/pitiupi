package pitiupi.chat.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.application.presence.UserPeerView;
import pitiupi.chat.GUI.ChatPanel;
import pitiupi.chat.GUI.LocalMemoryGame;
import pitiupi.chat.GUI.SwingMemoryGameUi;
import pitiupi.chat.control.MemoryGameController;
import pitiupi.chat.control.PrivateChatController;
import pitiupi.chat.control.PrivateChatHost;
import pitiupi.chat.net.ChatMessage;
import pitiupi.chat.net.LegacyMpChatMessage;
import pitiupi.chat.net.MemoryGameMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.models.User;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;

import javax.swing.SwingUtilities;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatPlugin extends Plugin implements PrivateChatHost {

    private ChatPanel chatPanel;
    private PluginWorkspace workspace;
    private ConnectionManager connectionManager;
    private PrivateChatController privateChatController;
    private MemoryGameController memoryGameController;

    /** Uma única thread: as jogadas do jogo da memória saem na mesma ordem em que foram feitas. */
    private final ExecutorService gameSender = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "chat-memory-game-sender");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.privateChatController = new PrivateChatController(connectionManager, this);
        this.chatPanel = new ChatPanel(connectionManager);
        chatPanel.setPrivateChatController(privateChatController);
        privateChatController.setUiSink(chatPanel);
        this.memoryGameController = new MemoryGameController(
                connectionManager::getUserName,
                LocalNetworkInfo::bestLocalIpv4,
                this::sendGameMessage,
                new SwingMemoryGameUi()
        );
        bindShellWorkspaceContent(chatPanel);
        installPeerActions(workspace.peerActions());
    }

    /** Jogo da memória: TCP direto para o IP do oponente, fora da thread do Swing. */
    private void sendGameMessage(MemoryGameMessage message, String ip) {
        gameSender.execute(() -> {
            boolean sent = false;
            try {
                sent = connectionManager.sendTcp(message, InetAddress.getByName(ip));
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
            if (!sent) {
                SwingUtilities.invokeLater(() -> memoryGameController.onSendFailed(message));
            }
        });
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "chat.focus",
                "Abrir sala geral",
                PeerActionTarget.ALL_PEERS,
                target -> {
                    chatPanel.focusGeneralRoom();
                    workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                }
        ));
        registrar.register(PeerAction.of(
                "chat.peer",
                "Conversar com este peer",
                PeerActionTarget.SINGLE_PEER,
                this::openChatForPeer
        ));
        registrar.register(PeerAction.of(
                "chat.private.tcp",
                "Ativar TCP privado",
                PeerActionTarget.SINGLE_PEER,
                target -> {
                    workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                    if (target instanceof PeerTarget.Single single) {
                        chatPanel.enablePrivateTcpForPeer(single.peer());
                    }
                }
        ));
        registrar.register(PeerAction.of(
                "chat.memory",
                "Jogar Jogo da Memória",
                PeerActionTarget.SINGLE_PEER,
                this::inviteToMemoryGame
        ));
        registrar.register(PeerAction.of(
                "chat.memory.local",
                "Jogo da Memória (local, neste computador)",
                PeerActionTarget.ALL_PEERS,
                target -> LocalMemoryGame.open()
        ));
    }

    private void inviteToMemoryGame(PeerTarget target) {
        if (!(target instanceof PeerTarget.Single single) || single.peer().isSelf()) {
            return;
        }
        UserPeerView peer = single.peer();
        peer.getInetAddress().ifPresent(address -> memoryGameController.invite(
                PeerIpv4.fromInetAddress(address),
                peer.getDisplayName()
        ));
    }

    private void openChatForPeer(PeerTarget target) {
        if (!(target instanceof PeerTarget.Single single)) {
            return;
        }
        chatPanel.focusPeer(single.peer());
        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
    }

    private static User toUser(UserPeerView peer) {
        if (peer.isBroadcastTarget() || peer.isSelf()) {
            return null;
        }
        return peer.getInetAddress()
                .map(addr -> new User(peer.getDisplayName(), addr, System.currentTimeMillis(), peer.getInstanceId()))
                .orElse(null);
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(ChatMessage.class, LegacyMpChatMessage.class, MemoryGameMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof ChatMessage msg && chatPanel != null) {
            SwingUtilities.invokeLater(() -> chatPanel.receiveChatMessage(msg));
        } else if (message instanceof MemoryGameMessage game && memoryGameController != null) {
            SwingUtilities.invokeLater(() -> memoryGameController.onMessage(game));
        } else if (message instanceof LegacyMpChatMessage legacy && chatPanel != null) {
            SwingUtilities.invokeLater(() -> chatPanel.receiveLegacyMessage(
                    legacy.getTipoMensagem(),
                    legacy.getNome(),
                    legacy.getIp(),
                    legacy.getMessage()
            ));
        }
    }

    @Override
    public boolean receivePrivateConnection(TcpConnection connection) {
        SwingUtilities.invokeLater(() -> privateChatController.connectionReceived(connection));
        return true;
    }

    @Override
    public boolean receiveBytes(TcpConnection connection, byte[] bytes) {
        LegacyMpChatMessage legacy = parseLegacy(bytes);
        if (legacy == null) {
            return true;
        }
        privateChatController.receiveMessage(connection, legacy);
        return true;
    }

    @Override
    public boolean onSocketClosed(TcpConnection connection) {
        privateChatController.closeChat(connection);
        return true;
    }

    @Override
    public void openConnection(String ip) {
        connectionManager.connectPluginTcpAsync(ip, getIdentifier(), getIdentifier(), new TcpAttemptListener() {
            @Override
            public void onConnecting(String endpoint) {
                if (chatPanel != null) {
                    chatPanel.showTcpAttempt("Conectando em " + endpoint + "…");
                }
            }

            @Override
            public void onSuccess(TcpConnection connection) {
                if (chatPanel != null) {
                    chatPanel.clearTcpAttempt();
                }
                privateChatController.registerOutgoingConnection(ip, connection);
            }

            @Override
            public void onFailure(String message) {
                privateChatController.abortOutgoing(ip);
                if (chatPanel != null) {
                    chatPanel.failTcpAttempt(message);
                }
            }
        });
    }

    @Override
    public String getPluginIdentifier() {
        return getIdentifier();
    }

    private LegacyMpChatMessage parseLegacy(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            if (ois.readObject() instanceof LegacyMpChatMessage msg) {
                return msg;
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public String getDisplayName() {
        return "Conversa na rede";
    }

    @Override
    public String getAuthor() {
        return "Flávio Schiavoni";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.Chat.Example";
    }
}