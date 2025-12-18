import SwiftUI

enum ValidationState {
    case idle
    case validating
    case valid
    case invalid(String)
}

class FormDemoViewModel: ObservableObject {
    @Published var username = ""
    @Published var email = ""
    @Published var usernameState: ValidationState = .idle
    @Published var emailState: ValidationState = .idle
    @Published var validationCount = 0

    private var usernameDebouncer: SwiftDebouncer?
    private var emailDebouncer: SwiftDebouncer?
    private var currentUsername = ""
    private var currentEmail = ""

    init() {
        // Initialize debouncers with 500ms delay
        usernameDebouncer = SwiftDebouncer(delayMillis: 500) { [weak self] in
            self?.performUsernameValidation()
        }

        emailDebouncer = SwiftDebouncer(delayMillis: 500) { [weak self] in
            self?.performEmailValidation()
        }
    }

    func validateUsername(_ value: String) {
        currentUsername = value

        guard !value.isEmpty else {
            usernameState = .idle
            return
        }

        usernameState = .validating
        // Call debouncer instead of validating immediately
        usernameDebouncer?.call()
    }

    private func performUsernameValidation() {
        let value = currentUsername
        validationCount += 1

        // Simulate server validation
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            if value.count < 3 {
                self?.usernameState = .invalid("Username must be at least 3 characters")
            } else if value.lowercased() == "admin" {
                self?.usernameState = .invalid("Username 'admin' is taken")
            } else {
                self?.usernameState = .valid
            }
        }
    }

    func validateEmail(_ value: String) {
        currentEmail = value

        guard !value.isEmpty else {
            emailState = .idle
            return
        }

        emailState = .validating
        // Call debouncer instead of validating immediately
        emailDebouncer?.call()
    }

    private func performEmailValidation() {
        let value = currentEmail
        validationCount += 1

        // Simulate server validation
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
            let emailPredicate = NSPredicate(format:"SELF MATCHES %@", emailRegex)
            let isValid = emailPredicate.evaluate(with: value)

            if !isValid {
                self?.emailState = .invalid("Invalid email format")
            } else if value.lowercased().contains("test@test.com") {
                self?.emailState = .invalid("Email already registered")
            } else {
                self?.emailState = .valid
            }
        }
    }
}

struct FormDemoView: View {
    @StateObject private var viewModel = FormDemoViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    Text("Real-time Form Validation")
                        .font(.title2)
                        .bold()

                    Text("Type in the fields below. Validation uses debounce (500ms) to avoid spamming the server.")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        Text("Validation Count:")
                        Text("\(viewModel.validationCount)")
                            .bold()
                            .foregroundColor(.blue)
                    }
                    .font(.caption)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .cornerRadius(12)

                // Username Field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Username")
                        .font(.headline)

                    HStack {
                        TextField("Enter username", text: $viewModel.username)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .onChange(of: viewModel.username) { newValue in
                                // TODO: Add debounce here
                                viewModel.validateUsername(newValue)
                            }

                        validationIcon(for: viewModel.usernameState)
                    }

                    if case .validating = viewModel.usernameState {
                        HStack {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text("Validating...")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }

                    if case .invalid(let message) = viewModel.usernameState {
                        Text(message)
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    if case .valid = viewModel.usernameState {
                        Text("Username is available!")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                // Email Field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Email")
                        .font(.headline)

                    HStack {
                        TextField("Enter email", text: $viewModel.email)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)
                            .onChange(of: viewModel.email) { newValue in
                                // TODO: Add debounce here
                                viewModel.validateEmail(newValue)
                            }

                        validationIcon(for: viewModel.emailState)
                    }

                    if case .validating = viewModel.emailState {
                        HStack {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text("Validating...")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }

                    if case .invalid(let message) = viewModel.emailState {
                        Text(message)
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    if case .valid = viewModel.emailState {
                        Text("Email is valid!")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                Spacer()
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func validationIcon(for state: ValidationState) -> some View {
        switch state {
        case .idle:
            EmptyView()
        case .validating:
            ProgressView()
                .scaleEffect(0.8)
        case .valid:
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
        case .invalid:
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.red)
        }
    }
}

struct FormDemoView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            FormDemoView()
        }
    }
}
