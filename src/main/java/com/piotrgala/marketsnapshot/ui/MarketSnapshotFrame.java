package com.piotrgala.marketsnapshot.ui;

import com.piotrgala.marketsnapshot.export.SnapshotCsvExporter;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.presentation.SnapshotSortOption;
import com.piotrgala.marketsnapshot.service.MarketSnapshotService;
import com.piotrgala.marketsnapshot.service.SnapshotDataSource;
import com.piotrgala.marketsnapshot.service.SnapshotResult;
import com.piotrgala.marketsnapshot.view.SnapshotFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class MarketSnapshotFrame extends JFrame {

    private static final String[] TABLE_COLUMNS = {
            "Symbol",
            "Name",
            "Price (USD)",
            "24h",
            "7d",
            "30d",
            "30d vol",
            "Market cap"
    };
    private static final Dimension MINIMUM_FRAME_SIZE = new Dimension(980, 520);
    private static final Color SUCCESS_COLOR = new Color(24, 111, 61);
    private static final Color ERROR_COLOR = new Color(153, 27, 27);
    private static final Color WARNING_COLOR = new Color(176, 98, 19);
    private static final Color INFO_COLOR = new Color(58, 93, 130);
    private static final DateTimeFormatter LAST_UPDATED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter EXPORT_FILE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MarketSnapshotService marketSnapshotService;
    private final SnapshotCsvExporter snapshotCsvExporter;
    private final Map<Asset, JCheckBox> assetCheckBoxes;
    private final JComboBox<SnapshotSortOption> sortComboBox;
    private final JButton refreshButton;
    private final JButton exportButton;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private final JLabel lastUpdatedLabel;
    private List<AssetSnapshot> currentSnapshots;
    private SnapshotResult currentResult;

    public MarketSnapshotFrame() {
        super("market-snapshot-tool");
        this.marketSnapshotService = new MarketSnapshotService();
        this.snapshotCsvExporter = new SnapshotCsvExporter();
        this.assetCheckBoxes = createAssetCheckBoxes();
        this.sortComboBox = new JComboBox<>(SnapshotSortOption.values());
        this.refreshButton = new JButton("Refresh snapshot");
        this.exportButton = new JButton("Export CSV");
        this.tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.statusLabel = new JLabel("Click refresh to load a market snapshot.");
        this.lastUpdatedLabel = new JLabel("Data as of: not yet");
        this.currentSnapshots = List.of();
        this.currentResult = emptyResult();

        configureFrame();
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(null);
        sortComboBox.addActionListener(event -> applyCurrentSort());
        refreshButton.addActionListener(event -> refreshSnapshot());
        exportButton.addActionListener(event -> exportCurrentSnapshot());
        exportButton.setEnabled(false);
        refreshSnapshot();
    }

    private void configureFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(MINIMUM_FRAME_SIZE);
        setPreferredSize(MINIMUM_FRAME_SIZE);
        setLocationByPlatform(true);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildStatusPanel(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(0, 12));
        headerPanel.add(buildTitlePanel(), BorderLayout.NORTH);
        headerPanel.add(buildControlsPanel(), BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel buildTitlePanel() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Market Snapshot Tool");
        titleLabel.setFont(titleLabel.getFont().deriveFont(22f));

        JLabel subtitleLabel = new JLabel("Mini desktop UI for live crypto snapshots and quick sorting.");
        subtitleLabel.setForeground(Color.DARK_GRAY);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(subtitleLabel);
        return titlePanel;
    }

    private JPanel buildControlsPanel() {
        JPanel controlsPanel = new JPanel(new BorderLayout(0, 8));
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        controlsPanel.add(buildAssetSelectionPanel(), BorderLayout.NORTH);
        controlsPanel.add(buildActionPanel(), BorderLayout.SOUTH);
        return controlsPanel;
    }

    private JPanel buildAssetSelectionPanel() {
        JPanel assetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        assetPanel.add(new JLabel("Assets:"));

        for (JCheckBox checkBox : assetCheckBoxes.values()) {
            assetPanel.add(checkBox);
        }

        return assetPanel;
    }

    private JPanel buildActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.add(new JLabel("Sort by:"));
        actionPanel.add(sortComboBox);
        actionPanel.add(refreshButton);
        actionPanel.add(exportButton);
        return actionPanel;
    }

    private JScrollPane buildCenterPanel() {
        JTable snapshotTable = new JTable(tableModel);
        snapshotTable.setRowHeight(26);
        snapshotTable.setFillsViewportHeight(true);
        snapshotTable.getTableHeader().setReorderingAllowed(false);
        snapshotTable.setAutoCreateRowSorter(false);
        return new JScrollPane(snapshotTable);
    }

    private JPanel buildStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel.setForeground(INFO_COLOR);
        lastUpdatedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(lastUpdatedLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private Map<Asset, JCheckBox> createAssetCheckBoxes() {
        Map<Asset, JCheckBox> checkBoxes = new LinkedHashMap<>();
        EnumSet<Asset> defaultAssets = EnumSet.copyOf(Asset.defaultAssets());

        for (Asset asset : Asset.values()) {
            JCheckBox checkBox = new JCheckBox(asset.symbol().toUpperCase());
            checkBox.setSelected(defaultAssets.contains(asset));
            checkBoxes.put(asset, checkBox);
        }

        return checkBoxes;
    }

    private void refreshSnapshot() {
        List<Asset> selectedAssets = selectedAssets();
        if (selectedAssets.isEmpty()) {
            clearSnapshotView();
            setStatus("Select at least one asset before refreshing.", ERROR_COLOR);
            updateExportButtonState();
            return;
        }

        setLoadingState(true);
        setStatus("Refreshing market snapshot...", INFO_COLOR);

        new SwingWorker<SnapshotResult, Void>() {
            @Override
            protected SnapshotResult doInBackground() throws Exception {
                return marketSnapshotService.getSnapshotResult(selectedAssets);
            }

            @Override
            protected void done() {
                try {
                    SnapshotResult result = get();
                    currentResult = result;
                    currentSnapshots = result.snapshots();
                    applyCurrentSort();
                    lastUpdatedLabel.setText("Data as of: " + formatInstant(result.dataAsOf()));
                    updateStatusForResult(result, selectedAssets.size());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    clearSnapshotView();
                    setStatus("Refresh was interrupted.", ERROR_COLOR);
                } catch (ExecutionException exception) {
                    clearSnapshotView();
                    setStatus(resolveErrorMessage(exception.getCause()), ERROR_COLOR);
                } finally {
                    setLoadingState(false);
                    updateExportButtonState();
                }
            }
        }.execute();
    }

    private List<Asset> selectedAssets() {
        List<Asset> selectedAssets = new ArrayList<>();
        for (Map.Entry<Asset, JCheckBox> entry : assetCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedAssets.add(entry.getKey());
            }
        }
        return selectedAssets;
    }

    private void populateTable(List<AssetSnapshot> snapshots) {
        tableModel.setRowCount(0);

        for (AssetSnapshot snapshot : snapshots) {
            tableModel.addRow(new Object[]{
                    snapshot.symbol(),
                    snapshot.name(),
                    SnapshotFormatter.formatPrice(snapshot.currentPrice()),
                    SnapshotFormatter.formatPercent(snapshot.change24h()),
                    SnapshotFormatter.formatPercent(snapshot.return7d()),
                    SnapshotFormatter.formatPercent(snapshot.return30d()),
                    SnapshotFormatter.formatPercent(snapshot.realizedVolatility()),
                    SnapshotFormatter.formatMarketCap(snapshot.marketCap())
            });
        }
    }

    private void applyCurrentSort() {
        SnapshotSortOption sortOption = (SnapshotSortOption) sortComboBox.getSelectedItem();
        if (sortOption == null) {
            sortOption = SnapshotSortOption.MARKET_CAP;
        }

        populateTable(sortOption.sort(currentSnapshots));
    }

    private void clearSnapshotView() {
        currentSnapshots = List.of();
        currentResult = emptyResult();
        tableModel.setRowCount(0);
        updateWarningsTooltip(List.of());
        lastUpdatedLabel.setText("Data as of: not yet");
    }

    private SnapshotResult emptyResult() {
        return new SnapshotResult(List.of(), SnapshotDataSource.LIVE, Instant.now(), List.of());
    }

    private void setLoadingState(boolean isLoading) {
        refreshButton.setEnabled(!isLoading);
        for (JCheckBox checkBox : assetCheckBoxes.values()) {
            checkBox.setEnabled(!isLoading);
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Failed to refresh the market snapshot.";
        }
        return throwable.getMessage();
    }

    private String formatInstant(Instant instant) {
        return LAST_UPDATED_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private void updateStatusForResult(SnapshotResult result, int requestedAssetsCount) {
        updateWarningsTooltip(result.warnings());

        if (result.hasWarnings()) {
            if (result.snapshots().isEmpty()) {
                setStatus("No assets loaded. " + result.warnings().size() + " warning(s).", ERROR_COLOR);
                return;
            }

            setStatus(
                    "Loaded " + result.snapshots().size() + " of " + requestedAssetsCount
                            + " assets from " + result.dataSource().label()
                            + " data. " + result.warnings().size() + " warning(s).",
                    WARNING_COLOR
            );
            return;
        }

        setStatus(
                "Loaded " + result.snapshots().size() + " assets from "
                        + result.dataSource().label() + " data.",
                SUCCESS_COLOR
        );
    }

    private void updateWarningsTooltip(List<String> warnings) {
        if (warnings.isEmpty()) {
            statusLabel.setToolTipText(null);
            return;
        }

        StringBuilder tooltip = new StringBuilder("<html>");
        for (String warning : warnings) {
            tooltip.append(warning).append("<br>");
        }
        tooltip.append("</html>");
        statusLabel.setToolTipText(tooltip.toString());
    }

    private void updateExportButtonState() {
        exportButton.setEnabled(!currentSnapshots.isEmpty());
    }

    private void exportCurrentSnapshot() {
        if (currentSnapshots.isEmpty()) {
            setStatus("Nothing to export yet. Refresh a snapshot first.", ERROR_COLOR);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export snapshot to CSV");
        chooser.setSelectedFile(new java.io.File("market-snapshot-"
                + EXPORT_FILE_FORMATTER.format(LocalDateTime.now()) + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path targetPath = chooser.getSelectedFile().toPath();
        if (!targetPath.getFileName().toString().toLowerCase().endsWith(".csv")) {
            targetPath = targetPath.resolveSibling(targetPath.getFileName() + ".csv");
        }

        try {
            SnapshotSortOption sortOption = (SnapshotSortOption) sortComboBox.getSelectedItem();
            if (sortOption == null) {
                sortOption = SnapshotSortOption.MARKET_CAP;
            }

            snapshotCsvExporter.export(
                    targetPath,
                    sortOption.sort(currentSnapshots),
                    currentResult.dataSource(),
                    currentResult.dataAsOf()
            );
            setStatus("Exported " + currentSnapshots.size() + " rows to " + targetPath.getFileName() + ".", SUCCESS_COLOR);
        } catch (IOException exception) {
            setStatus("Export failed: " + exception.getMessage(), ERROR_COLOR);
        }
    }
}
