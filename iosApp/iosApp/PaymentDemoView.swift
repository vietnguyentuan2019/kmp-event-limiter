import SwiftUI

struct Transaction: Identifiable {
    let id = UUID()
    let amount: Double
    let timestamp: Date
    let status: String
}

class PaymentDemoViewModel: ObservableObject {
    @Published var amount = ""
    @Published var transactions: [Transaction] = []
    @Published var isProcessing = false
    @Published var attemptCount = 0
    @Published var blockedCount = 0
    @Published var successCount = 0

    private var throttler: SwiftThrottler?

    var protectionRate: Double {
        guard attemptCount > 0 else { return 0 }
        return Double(blockedCount) / Double(attemptCount) * 100
    }

    init() {
        // Initialize throttler with 2000ms delay (to prevent rapid clicking)
        throttler = SwiftThrottler(delayMillis: 2000) { [weak self] in
            self?.performPayment()
        }
    }

    func processPayment() {
        attemptCount += 1

        // Manual check for processing state
        guard !isProcessing else {
            blockedCount += 1
            return
        }

        // Call throttler which will execute immediately first time,
        // then block subsequent calls for 2 seconds
        throttler?.call()
    }

    private func performPayment() {
        guard !isProcessing else {
            blockedCount += 1
            return
        }

        isProcessing = true

        // Simulate payment processing
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else { return }

            if let amountValue = Double(self.amount), amountValue > 0 {
                let transaction = Transaction(
                    amount: amountValue,
                    timestamp: Date(),
                    status: "Success"
                )
                self.transactions.insert(transaction, at: 0)
                self.successCount += 1
                self.amount = ""
            }

            self.isProcessing = false
        }
    }

    func reset() {
        transactions.removeAll()
        attemptCount = 0
        blockedCount = 0
        successCount = 0
        amount = ""
    }
}

struct PaymentDemoView: View {
    @StateObject private var viewModel = PaymentDemoViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    Text("Payment Protection")
                        .font(.title2)
                        .bold()

                    Text("Try clicking the payment button multiple times rapidly. AsyncThrottler prevents duplicate transactions.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .cornerRadius(12)

                // Statistics
                VStack(spacing: 12) {
                    statRow("Total Attempts", value: "\(viewModel.attemptCount)", color: .blue)
                    statRow("Blocked", value: "\(viewModel.blockedCount)", color: .orange)
                    statRow("Successful", value: "\(viewModel.successCount)", color: .green)
                    statRow("Protection Rate", value: String(format: "%.1f%%", viewModel.protectionRate), color: .purple)
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                // Payment Form
                VStack(spacing: 16) {
                    Text("Make a Payment")
                        .font(.headline)

                    HStack {
                        Text("$")
                            .font(.title2)
                        TextField("0.00", text: $viewModel.amount)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .keyboardType(.decimalPad)
                    }

                    Button(action: { viewModel.processPayment() }) {
                        HStack {
                            if viewModel.isProcessing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                Text("Processing...")
                            } else {
                                Image(systemName: "creditcard")
                                Text("Process Payment")
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(viewModel.isProcessing ? Color.gray : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(viewModel.amount.isEmpty)

                    Button(action: { viewModel.reset() }) {
                        Text("Reset")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .foregroundColor(.red)
                            .cornerRadius(12)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                // Transaction History
                if !viewModel.transactions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Transaction History")
                            .font(.headline)

                        ForEach(viewModel.transactions) { transaction in
                            transactionRow(transaction)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                }

                Spacer()
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func statRow(_ label: String, value: String, color: Color) -> some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .bold()
                .foregroundColor(color)
        }
    }

    private func transactionRow(_ transaction: Transaction) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(String(format: "$%.2f", transaction.amount))
                    .font(.headline)
                Text(transaction.timestamp, style: .time)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Text(transaction.status)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.green.opacity(0.1))
                .foregroundColor(.green)
                .cornerRadius(8)
        }
        .padding()
        .background(Color(.systemGroupedBackground))
        .cornerRadius(8)
    }
}

struct PaymentDemoView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            PaymentDemoView()
        }
    }
}
