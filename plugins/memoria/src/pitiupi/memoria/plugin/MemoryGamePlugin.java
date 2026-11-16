package pitiupi.memoria.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.memoria.GUI.LocalMemoryGame;
import pitiupi.memoria.GUI.MemoryGamePanel;
import pitiupi.memoria.GUI.SwingMemoryGameUi;
import pitiupi.memoria.control.MemoryGameController;
import pitiupi.memoria.net.MemoryGameMessage;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plugin do Jogo da Memória para duas pessoas.
 *
 * <p>Botão "Jogo da Memória" na aba Plugins (categoria Jogos). A aba do plugin permite jogar
 * neste computador (dois tabuleiros) ou convidar alguém pelo IP. Também dá para convidar
 * clicando no nome da pessoa na lista "Usuários nesta rede" → "Jogar Jogo da Memória".</p>
 */
public class MemoryGamePlugin extends Plugin {

    private ConnectionManager connectionManager;
    private PluginWorkspace workspace;
    private MemoryGameController controller;

    /** Uma única thread: as jogadas saem na mesma ordem em que foram feitas. */
    private final ExecutorService sender = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "memoria-sender");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.controller = new MemoryGameController(
                connectionManager::getUserName,
                LocalNetworkInfo::bestLocalIpv4,
                this::send,
                new SwingMemoryGameUi()
        );
        bindShellWorkspaceContent(new MemoryGamePanel(
                LocalMemoryGame::open,
                ip -> controller.invite(ip, ip)
        ));
        installPeerActions(workspace.peerActions());
    }

    /** TCP direto para o IP do oponente, fora da thread do Swing. */
    private void send(MemoryGameMessage message, String ip) {
        sender.execute(() -> {
            boolean sent = false;
            try {
                sent = connectionManager.sendTcp(message, InetAddress.getByName(ip));
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
            if (!sent) {
                SwingUtilities.invokeLater(() -> controller.onSendFailed(message));
            }
        });
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "memoria.invite",
                "Jogar Jogo da Memória",
                PeerActionTarget.SINGLE_PEER,
                this::inviteFromPeerList
        ));
    }

    private void inviteFromPeerList(PeerTarget target) {
        if (!(target instanceof PeerTarget.Single single) || single.peer().isSelf()) {
            return;
        }
        UserPeerView peer = single.peer();
        peer.getInetAddress().ifPresent(address -> controller.invite(
                PeerIpv4.fromInetAddress(address),
                peer.getDisplayName()
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(MemoryGameMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof MemoryGameMessage game && controller != null) {
            SwingUtilities.invokeLater(() -> controller.onMessage(game));
        }
    }

    @Override
    public String getName() {
        return "Memoria";
    }

    @Override
    public String getDisplayName() {
        return "Jogo da Memória";
    }

    @Override
    public String getAuthor() {
        return "Bárbara Neri";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.Memoria";
    }
}
