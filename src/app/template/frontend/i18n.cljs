(ns app.template.frontend.i18n
  (:require
    [tongue.core :as tongue]
    [uix.re-frame :refer [use-subscribe]]))

;; ---------------------------------------------------------------------------
;; Dictionary
;; Bosnian (:bs) is the default; English (:en) is the fallback.
;; Plain functions are used for interpolated strings: (t :key arg1 arg2).
;; Developer/console messages are intentionally kept in English.
;; ---------------------------------------------------------------------------

(def dicts
  {:bs
   {;; Common actions / labels
    :common/cancel               "Odustani"
    :common/confirm              "Potvrdi"
    :common/save-changes         "Spremi promjene"
    :common/error                "Greška"
    :common/success              "Uspješno"
    :common/close                "Zatvori"
    :common/email                "E-mail"
    :common/email-address        "E-mail adresa"
    :common/email-placeholder    "Unesite e-mail adresu"
    :common/password             "Lozinka"
    :common/password-placeholder "Unesite lozinku"
    :common/role                 "Uloga"
    :common/sign-out             "Odjava"
    :common/sign-in              "Prijavite se"
    :common/or                   "ILI"
    :common/and                  "i"
    :common/terms                "Uvjeti korištenja"
    :common/privacy              "Politika privatnosti"
    :common/full-name            "Puno ime"
    :common/full-name-placeholder "Unesite puno ime"
    :common/dismiss              "Odbaci"
    :common/status               "Status"
    :common/actions              "Radnje"
    :common/organization         "Organizacija"
    :common/name                 "Ime"
    :common/membership           "Članstvo"
    :common/account              "Račun"
    :common/joined               "Datum pristupanja"

    ;; Form validation
    :validation/email-required      "E-mail je obavezan"
    :validation/email-invalid       "Unesite valjanu e-mail adresu"
    :validation/password-required   "Lozinka je obavezna"
    :validation/password-min        "Lozinka mora imati najmanje 10 znakova"
    :validation/full-name-required  "Puno ime je obavezno"
    :validation/full-name-min       "Puno ime mora imati najmanje 2 znaka"
    :validation/passwords-no-match  "Lozinke se ne podudaraju"

    ;; Login page
    :login/welcome            "Dobrodošli"
    :login/welcome-back       "Dobrodošli nazad!"
    :login/subtitle           "Prijavite se na vaš račun da biste nastavili"
    :login/already-signed-in  "Već ste prijavljeni."
    :login/continue-to-app    "Nastavi na aplikaciju"
    :login/checking-auth      "Provjera autentikacije..."
    :login/sign-in-with-email "Prijavite se e-mailom"
    :login/forgot-password    "Zaboravili ste lozinku?"
    :login/sign-in            "Prijava"
    :login/signing-in         "Prijava u toku..."
    :login/continue-google    "Nastavite s Googleom"
    :login/continue-github    "Nastavite s GitHubom"
    :login/agree-text         "Prijavom, prihvaćate naše"
    :login/no-account         "Nemate račun?"
    :login/sign-up            "Registrirajte se"

    ;; Register page
    :register/success-title               "Registracija uspješna"
    :register/success-subtitle            "Vaš račun je kreiran!"
    :register/check-email                 "Provjerite e-mail"
    :register/complete                    "Registracija dovršena!"
    :register/continue-login              "Nastavi na prijavu"
    :register/go-home                     "Idi na početnu stranicu"
    :register/title                       "Kreirajte račun"
    :register/subtitle                    "Registrirajte se kako biste počeli koristiti vaš račun"
    :register/confirm-password            "Potvrdite lozinku"
    :register/confirm-password-placeholder "Potvrdite lozinku"
    :register/password-placeholder        "Unesite sigurnu lozinku (min. 10 znakova)"
    :register/submit                      "Kreirajte račun"
    :register/submitting                  "Kreiranje računa..."
    :register/have-account                "Već imate račun?"

    ;; Forgot-password page
    :forgot-password/title               "Zaboravljena lozinka"
    :forgot-password/subtitle            "Unesite e-mail za primanje uputa za resetiranje"
    :forgot-password/check-email-title   "Provjerite e-mail"
    :forgot-password/check-email-message "Ako postoji račun s ovim e-mailom, primit ćete upute za resetiranje lozinke."
    :forgot-password/back-to-login       "Povratak na prijavu"
    :forgot-password/submit              "Pošalji link za resetiranje"
    :forgot-password/submitting          "Slanje..."
    :forgot-password/back-link           "← Povratak na prijavu"

    ;; Confirm dialog defaults
    :confirm/action-title "Potvrdi radnju"

    ;; Messages component
    :messages/error   "Greška"
    :messages/success "Uspješno"

    ;; Navigation sidebar
    :nav/app-title           "Troškovi"
    :nav/dashboard           "Nadzorna ploča"
    :nav/receipts            "Računi"
    :nav/expenses            "Troškovi"
    :nav/expense-items       "Stavke troškova"
    :nav/upload              "Učitaj"
    :nav/reports             "Izvještaji"
    :nav/unmapped-aliases    "Neizmapirana pravila"
    :nav/suppliers           "Dobavljači"
    :nav/payers              "Platitelji"
    :nav/payer-types         "Vrste platitelja"
    :nav/articles            "Artikli"
    :nav/manufacturers       "Proizvođači"
    :nav/categories          "Kategorije"
    :nav/expense-categories  "Kategorije troškova"
    :nav/subcategories       "Podkategorije"
    :nav/stores              "Trgovine"
    :nav/cities              "Gradovi"
    :nav/article-aliases     "Pravila artikala"
    :nav/supplier-aliases    "Pravila dobavljača"
    :nav/store-aliases       "Pravila trgovina"
    :nav/members             "Članovi"
    :nav/impersonation       "Imitacija"
    :nav/log-out             "Odjava"
    :nav/section-expenses    "Troškovi"
    :nav/section-operations  "Operacije"
    :nav/section-reference   "Reference"
    :nav/section-workspace   "Radni prostor"

    ;; Tenant pages
    :tenant/welcome          (fn [name] (str "Dobrodošli, " name "!"))
    :tenant/select-workspace "Odaberite radni prostor"
    :tenant/no-workspaces    "Nema dostupnih radnih prostora. Kontaktirajte administratora."
    :tenant/members-title    "Članovi"
    :tenant/members-subtitle (fn [workspace] (str "Upravljajte članovima " workspace))
    :tenant/current-members  "Trenutni članovi"
    :tenant/no-members       "Još nema članova."
    :tenant/invitations      "Pozivnice"
    :tenant/sent             "Poslano"
    :tenant/send-invite      "Pošalji pozivnicu"
    :tenant/resend           "Ponovo pošalji"
    :tenant/revoke           "Opozovi"
    :tenant/transfer         "Prenesi"
    :tenant/role-viewer      "Pregledač"
    :tenant/role-member      "Član"
    :tenant/role-admin       "Administrator"

    :tenant/disable-member-title "Onemogući člana"
    :tenant/disable-member-msg   (fn [name]
                                   (str "Onemogućiti " name " za ovaj radni prostor? "
                                     "Izgubit će pristup, ali se može ponovo aktivirati."))
    :tenant/disable              "Onemogući"
    :tenant/enable-member-title  "Omogući člana"
    :tenant/enable-member-msg    (fn [name]
                                   (str "Ponovo aktivirati " name " za ovaj radni prostor?"))
    :tenant/enable               "Omogući"

    ;; Tooltip / disabled-reason strings for member management
    :tenant/reason-editing-disabled    "Uređivanje onemogućeno"
    :tenant/reason-owner-role          "Uloga vlasnika se ne može promijeniti"
    :tenant/reason-membership-disabled "Članstvo je onemogućeno"
    :tenant/reason-no-edit-permission  "Nemate dozvolu za uređivanje ovog člana"
    :tenant/reason-deletion-disabled   "Brisanje onemogućeno"
    :tenant/reason-owner-remove        "Vlasnik se ne može ukloniti"
    :tenant/reason-no-remove-permission "Nemate dozvolu za uklanjanje ovog člana"

    ;; Reset password page
    :reset-password/missing-token-title    "Nedostaje token za resetiranje"
    :reset-password/missing-token-desc     "Nije pronađen token za resetiranje lozinke. Koristite link iz e-maila."
    :reset-password/request-new-link       "Zahtijevajte novi link"
    :reset-password/verifying              "Provjera linka za resetiranje..."
    :reset-password/invalid-title         "Nevažeći ili istekli link"
    :reset-password/invalid-default       "Ovaj link za resetiranje lozinke je nevažeći ili je istekao."
    :reset-password/invalid-request-new   "Molimo zatražite novi link za resetiranje lozinke."
    :reset-password/success-title         "Lozinka uspješno resetirana"
    :reset-password/success-desc          "Vaša lozinka je ažurirana. Možete se sada prijaviti s novom lozinkom."
    :reset-password/form-title            "Resetirajte lozinku"
    :reset-password/form-subtitle         "Unesite novu lozinku"
    :reset-password/new-password          "Nova lozinka"
    :reset-password/new-password-ph       "Unesite novu lozinku (min. 10 znakova)"
    :reset-password/confirm-password      "Potvrdite lozinku"
    :reset-password/confirm-password-ph   "Potvrdite novu lozinku"
    :reset-password/submit                "Resetirajte lozinku"
    :reset-password/submitting            "Resetiranje..."

    ;; Invitation accept page
    :invitation/title                "Pozivnica za radni prostor"
    :invitation/no-token             "Nije pronađen token pozivnice. Provjerite link pozivnice."
    :invitation/go-to-dashboard      "Idi na nadzornu ploču"
    :invitation/sign-in-required     "Prijavite se kako biste prihvatili ovu pozivnicu."
    :invitation/expired-or-used      "Pozivnica možda je istekla ili je već korištena."
    :invitation/ready-text           "Pozvani ste da se pridružite radnom prostoru. Kliknite ispod za prihvatanje."
    :invitation/accept               "Prihvati pozivnicu"

    ;; Email verification page
    :email-verification/verified-title    "E-mail potvrđen!"
    :email-verification/verified-desc     "Vaš e-mail je uspješno potvrđen. Sada imate pun pristup svim funkcijama."
    :email-verification/continue-to-app  "Nastavi na aplikaciju"
    :email-verification/failed-title     "Provjera nije uspjela"
    :email-verification/back-to-login    "Povratak na prijavu"
    :email-verification/request-new-link "Zahtijevajte novi link"
    :email-verification/err-not-found    "Link za provjeru nije valjan."
    :email-verification/err-expired      "Link za provjeru je istekao."
    :email-verification/err-already-used "Ovaj link za provjeru je već korišten."
    :email-verification/err-too-many     "Previše pokušaja provjere. Zatražite novi link."
    :email-verification/err-db          "Došlo je do tehničke greške. Pokušajte ponovo."
    :email-verification/err-default     "Greška pri provjeri."

    ;; Verify email success page
    :verify-email/verified-title   "E-mail potvrđen!"
    :verify-email/verified-subtitle "Vaša e-mail adresa je uspješno potvrđena."
    :verify-email/complete-title   "Provjera dovršena!"
    :verify-email/sign-in          "Prijavite se na vaš račun"
    :verify-email/go-home          "Idi na početnu stranicu"
    :verify-email/footer-message   "Vaš račun je spreman za korištenje"

    ;; Impersonation grants page
    :impersonation/page-title      "Imitacijska ovlaštenja"
    :impersonation/manage-access   (fn [workspace] (str "Upravljajte administratorskim pristupom " workspace))
    :impersonation/owners-only     "Samo vlasnici zakupca mogu upravljati imitacijskim ovlaštenjima."
    :impersonation/dismiss         "Odbaci"
    :impersonation/explanation     "Imitacijska ovlaštenja omogućuju administratorima platforme pristup vašem radnom prostoru s određenom ulogom za potrebe podrške. Možete opozvati pristup u bilo kom trenutku."
    :impersonation/grant-access    "Dodijelite administratorski pristup"
    :impersonation/admin-email     "E-mail administratora"
    :impersonation/grant-submit    "Dodijelite pristup"
    :impersonation/active-grants   "Aktivna ovlaštenja"
    :impersonation/no-grants       "Još nema imitacijskih ovlaštenja."
    :impersonation/col-admin-email "E-mail administratora"
    :impersonation/col-role        "Uloga"
    :impersonation/col-status      "Status"
    :impersonation/col-created     "Kreirano"
    :impersonation/col-actions     "Radnje"
    :impersonation/confirm-revoke  "Potvrdi"
    :impersonation/revoke          "Opozovi"

    ;; Expense dashboard page
    :dashboard/greeting            (fn [name] (str "Zdravo, " name "!"))
    :dashboard/subtitle            "Pregled vaših troškova"
    :dashboard/upload-receipt      "📷 Učitaj račun"
    :dashboard/view-all            "Prikaži sve"
    :dashboard/load-error          "Nije moguće učitati podatke o troškovima."
    :dashboard/this-month          "Ovaj mjesec"
    :dashboard/posted-expenses     "objavljeni troškovi"
    :dashboard/total               "Ukupno"
    :dashboard/all-time            "sve vrijeme"
    :dashboard/last-30-days        "Zadnjih 30 dana"
    :dashboard/expenses-created    "troškova kreirano"
    :dashboard/avg-per-expense     "Prosjek po trošku"
    :dashboard/currency-label      (fn [c] (str "valuta " c))
    :dashboard/recent-expenses     "Nedavni troškovi"
    :dashboard/view-all-link       "Prikaži sve →"
    :dashboard/no-expenses         "Još nema troškova. Pokušajte dodati jedan!"
    :dashboard/quick-actions       "Brze radnje"
    :dashboard/upload-receipt-desc "Skenirajte ili učitajte račun"
    :dashboard/add-expense         "Dodajte trošak"
    :dashboard/add-expense-desc    "Ručni unos troška"
    :dashboard/view-reports        "Pregledajte izvještaje"
    :dashboard/view-reports-desc   "Mjesečni sažeci"
    :dashboard/unmapped-aliases    "Neizmapirana pravila"
    :dashboard/unmapped-aliases-desc "Masovno mapirajte pravila na artikle"
    :dashboard/vs-last-month       "u odnosu na prošli mjesec"

    ;; Language switcher labels
    :lang/bs "BS"
    :lang/en "EN"

    :tongue/fallback :en}

   ;; ---------------------------------------------------------------------------
   :en
   {;; Common actions / labels
    :common/cancel               "Cancel"
    :common/confirm              "Confirm"
    :common/save-changes         "Save changes"
    :common/error                "Error"
    :common/success              "Success"
    :common/close                "Close"
    :common/email                "Email"
    :common/email-address        "Email Address"
    :common/email-placeholder    "Enter your email address"
    :common/password             "Password"
    :common/password-placeholder "Enter your password"
    :common/role                 "Role"
    :common/sign-out             "Sign Out"
    :common/sign-in              "Sign In"
    :common/or                   "OR"
    :common/and                  "and"
    :common/terms                "Terms of Service"
    :common/privacy              "Privacy Policy"
    :common/full-name            "Full Name"
    :common/full-name-placeholder "Enter your full name"
    :common/dismiss              "Dismiss"
    :common/status               "Status"
    :common/actions              "Actions"
    :common/organization         "Organization"
    :common/name                 "Name"
    :common/membership           "Membership"
    :common/account              "Account"
    :common/joined               "Joined"

    ;; Form validation
    :validation/email-required      "Email is required"
    :validation/email-invalid       "Please enter a valid email address"
    :validation/password-required   "Password is required"
    :validation/password-min        "Password must be at least 10 characters"
    :validation/full-name-required  "Full name is required"
    :validation/full-name-min       "Full name must be at least 2 characters"
    :validation/passwords-no-match  "Passwords do not match"

    ;; Login page
    :login/welcome            "Welcome"
    :login/welcome-back       "Welcome Back!"
    :login/subtitle           "Sign in to your account to continue"
    :login/already-signed-in  "You are already signed in."
    :login/continue-to-app    "Continue to App"
    :login/checking-auth      "Checking authentication..."
    :login/sign-in-with-email "Sign in with Email"
    :login/forgot-password    "Forgot password?"
    :login/sign-in            "Sign In"
    :login/signing-in         "Signing in..."
    :login/continue-google    "Continue with Google"
    :login/continue-github    "Continue with GitHub"
    :login/agree-text         "By signing in, you agree to our"
    :login/no-account         "Don't have an account?"
    :login/sign-up            "Sign up"

    ;; Register page
    :register/success-title               "Registration Successful"
    :register/success-subtitle            "Your account has been created!"
    :register/check-email                 "Check Your Email"
    :register/complete                    "Registration Complete!"
    :register/continue-login              "Continue to Login"
    :register/go-home                     "Go to Homepage"
    :register/title                       "Create Account"
    :register/subtitle                    "Sign up to get started with your account"
    :register/confirm-password            "Confirm Password"
    :register/confirm-password-placeholder "Confirm your password"
    :register/password-placeholder        "Enter a secure password (min. 10 characters)"
    :register/submit                      "Create Account"
    :register/submitting                  "Creating Account..."
    :register/have-account                "Already have an account?"

    ;; Forgot-password page
    :forgot-password/title               "Forgot Password"
    :forgot-password/subtitle            "Enter your email to receive reset instructions"
    :forgot-password/check-email-title   "Check Your Email"
    :forgot-password/check-email-message "If an account exists with this email, you'll receive password reset instructions."
    :forgot-password/back-to-login       "Back to Login"
    :forgot-password/submit              "Send Reset Link"
    :forgot-password/submitting          "Sending..."
    :forgot-password/back-link           "← Back to Login"

    ;; Confirm dialog defaults
    :confirm/action-title "Confirm Action"

    ;; Messages component
    :messages/error   "Error"
    :messages/success "Success"

    ;; Navigation sidebar
    :nav/app-title           "Expenses"
    :nav/dashboard           "Dashboard"
    :nav/receipts            "Receipts"
    :nav/expenses            "Expenses"
    :nav/expense-items       "Expense Items"
    :nav/upload              "Upload"
    :nav/reports             "Reports"
    :nav/unmapped-aliases    "Unmapped Aliases"
    :nav/suppliers           "Suppliers"
    :nav/payers              "Payers"
    :nav/payer-types         "Payer Types"
    :nav/articles            "Articles"
    :nav/manufacturers       "Manufacturers"
    :nav/categories          "Categories"
    :nav/expense-categories  "Expense Categories"
    :nav/subcategories       "Subcategories"
    :nav/stores              "Stores"
    :nav/cities              "Cities"
    :nav/article-aliases     "Article Aliases"
    :nav/supplier-aliases    "Supplier Aliases"
    :nav/store-aliases       "Store Aliases"
    :nav/members             "Members"
    :nav/impersonation       "Impersonation"
    :nav/log-out             "Log Out"
    :nav/section-expenses    "Expenses"
    :nav/section-operations  "Operations"
    :nav/section-reference   "Reference"
    :nav/section-workspace   "Workspace"

    ;; Tenant pages
    :tenant/welcome          (fn [name] (str "Welcome, " name "!"))
    :tenant/select-workspace "Select a Workspace"
    :tenant/no-workspaces    "No workspaces available. Contact your administrator."
    :tenant/members-title    "Members"
    :tenant/members-subtitle (fn [workspace] (str "Manage members of " workspace))
    :tenant/current-members  "Current Members"
    :tenant/no-members       "No members yet."
    :tenant/invitations      "Invitations"
    :tenant/sent             "Sent"
    :tenant/send-invite      "Send Invite"
    :tenant/resend           "Resend"
    :tenant/revoke           "Revoke"
    :tenant/transfer         "Transfer"
    :tenant/role-viewer      "Viewer"
    :tenant/role-member      "Member"
    :tenant/role-admin       "Admin"

    :tenant/disable-member-title "Disable member"
    :tenant/disable-member-msg   (fn [name]
                                   (str "Disable " name " for this tenant? "
                                     "They will lose access, but can be re-enabled later."))
    :tenant/disable              "Disable"
    :tenant/enable-member-title  "Enable member"
    :tenant/enable-member-msg    (fn [name]
                                   (str "Re-enable " name " for this tenant?"))
    :tenant/enable               "Enable"

    ;; Tooltip / disabled-reason strings
    :tenant/reason-editing-disabled     "Editing disabled"
    :tenant/reason-owner-role           "Owner role cannot be changed"
    :tenant/reason-membership-disabled  "Membership is disabled"
    :tenant/reason-no-edit-permission   "You don't have permission to edit this member"
    :tenant/reason-deletion-disabled    "Deletion disabled"
    :tenant/reason-owner-remove         "Owner cannot be removed"
    :tenant/reason-no-remove-permission "You don't have permission to remove this member"

    ;; Reset password page
    :reset-password/missing-token-title    "Missing Reset Token"
    :reset-password/missing-token-desc     "No password reset token was provided. Please use the link from your email."
    :reset-password/request-new-link       "Request New Link"
    :reset-password/verifying              "Verifying reset link..."
    :reset-password/invalid-title         "Invalid or Expired Link"
    :reset-password/invalid-default       "This password reset link is invalid or has expired."
    :reset-password/invalid-request-new   "Please request a new password reset link."
    :reset-password/success-title         "Password Reset Successfully"
    :reset-password/success-desc          "Your password has been updated. You can now sign in with your new password."
    :reset-password/form-title            "Reset Password"
    :reset-password/form-subtitle         "Enter your new password"
    :reset-password/new-password          "New Password"
    :reset-password/new-password-ph       "Enter new password (min. 10 characters)"
    :reset-password/confirm-password      "Confirm Password"
    :reset-password/confirm-password-ph   "Confirm new password"
    :reset-password/submit                "Reset Password"
    :reset-password/submitting            "Resetting..."

    ;; Invitation accept page
    :invitation/title                "Workspace Invitation"
    :invitation/no-token             "No invitation token found. Please check your invitation link."
    :invitation/go-to-dashboard      "Go to Dashboard"
    :invitation/sign-in-required     "Please sign in to accept this invitation."
    :invitation/expired-or-used      "The invitation may have expired or already been used."
    :invitation/ready-text           "You've been invited to join a workspace. Click below to accept."
    :invitation/accept               "Accept Invitation"

    ;; Email verification page
    :email-verification/verified-title    "Email Verified!"
    :email-verification/verified-desc     "Your email has been successfully verified. You now have full access to all features."
    :email-verification/continue-to-app  "Continue to App"
    :email-verification/failed-title     "Verification Failed"
    :email-verification/back-to-login    "Back to Login"
    :email-verification/request-new-link "Request New Link"
    :email-verification/err-not-found    "The verification link is invalid."
    :email-verification/err-expired      "The verification link has expired."
    :email-verification/err-already-used "This verification link has already been used."
    :email-verification/err-too-many     "Too many verification attempts. Please request a new link."
    :email-verification/err-db          "A technical error occurred. Please try again."
    :email-verification/err-default     "An error occurred during verification."

    ;; Verify email success page
    :verify-email/verified-title   "Email Verified!"
    :verify-email/verified-subtitle "Your email address has been successfully verified."
    :verify-email/complete-title   "Verification Complete!"
    :verify-email/sign-in          "Sign In to Your Account"
    :verify-email/go-home          "Go to Homepage"
    :verify-email/footer-message   "Your account is now ready to use"

    ;; Impersonation grants page
    :impersonation/page-title      "Impersonation Grants"
    :impersonation/manage-access   (fn [workspace] (str "Manage admin access to " workspace))
    :impersonation/owners-only     "Only tenant owners can manage impersonation grants."
    :impersonation/dismiss         "Dismiss"
    :impersonation/explanation     "Impersonation grants allow platform administrators to access your workspace with a specific role for support and troubleshooting purposes. You can revoke access at any time."
    :impersonation/grant-access    "Grant Admin Access"
    :impersonation/admin-email     "Admin Email"
    :impersonation/grant-submit    "Grant Access"
    :impersonation/active-grants   "Active Grants"
    :impersonation/no-grants       "No impersonation grants yet."
    :impersonation/col-admin-email "Admin Email"
    :impersonation/col-role        "Role"
    :impersonation/col-status      "Status"
    :impersonation/col-created     "Created"
    :impersonation/col-actions     "Actions"
    :impersonation/confirm-revoke  "Confirm"
    :impersonation/revoke          "Revoke"

    ;; Expense dashboard page
    :dashboard/greeting            (fn [name] (str "Hello, " name "!"))
    :dashboard/subtitle            "Here's your expense overview"
    :dashboard/upload-receipt      "📷 Upload Receipt"
    :dashboard/view-all            "View All"
    :dashboard/load-error          "Unable to load expense data."
    :dashboard/this-month          "This Month"
    :dashboard/posted-expenses     "posted expenses"
    :dashboard/total               "Total"
    :dashboard/all-time            "all time"
    :dashboard/last-30-days        "Last 30 days"
    :dashboard/expenses-created    "expenses created"
    :dashboard/avg-per-expense     "Avg per expense"
    :dashboard/currency-label      (fn [c] (str "currency " c))
    :dashboard/recent-expenses     "Recent Expenses"
    :dashboard/view-all-link       "View all →"
    :dashboard/no-expenses         "No expenses yet. Try adding one!"
    :dashboard/quick-actions       "Quick Actions"
    :dashboard/upload-receipt-desc "Scan or upload a receipt"
    :dashboard/add-expense         "Add Expense"
    :dashboard/add-expense-desc    "Manual expense entry"
    :dashboard/view-reports        "View Reports"
    :dashboard/view-reports-desc   "Monthly summaries"
    :dashboard/unmapped-aliases    "Unmapped Aliases"
    :dashboard/unmapped-aliases-desc "Bulk-map aliases to articles"
    :dashboard/vs-last-month       "vs last month"

    ;; Language switcher labels
    :lang/bs "BS"
    :lang/en "EN"}})

;; ---------------------------------------------------------------------------
;; translate fn and use-t hook
;; ---------------------------------------------------------------------------

(def translate (tongue/build-translate dicts))

(defn use-t
  "React hook: subscribes to [:locale] and returns a curried translate fn.
   Components calling (use-t) re-render automatically on locale change.
   Usage: (let [t (use-t)] (t :login/welcome))"
  []
  (let [locale (use-subscribe [:locale])]
    (fn [key & args]
      (apply translate locale key args))))

(comment
  (translate :bs :login/welcome)        ;=> "Dobrodošli"
  (translate :en :login/welcome)        ;=> "Welcome"
  (translate :bs :tenant/welcome "Ana") ;=> "Dobrodošli, Ana!"
  :rcf)
