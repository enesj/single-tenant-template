<!-- ai: {:tags [:validation :guide :single-tenant] :kind :guide} -->

# Validation Implementation Guide

How to add and use validation metadata in the single-tenant admin app.

## 1) Add metadata in `resources/db/models.edn`
```edn
:users {:fields [
  [:email [:varchar 255]
   {:null false
    :validation {:type :email
                 :constraints {:pattern "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                               :max-length 255}
                 :messages {:invalid "Please enter a valid email address"
                            :required "Email is required"}
                 :ui {:input-type "email"
                      :placeholder "Enter your email address"
                      :autocomplete "email"}}}]

  [:full_name [:varchar 255]
   {:validation {:type :text
                 :constraints {:min-length 2
                               :max-length 255
                               :pattern "^[A-Za-z\\s\\-'\\.]+$"}
                 :messages {:invalid "Name can only contain letters, spaces, hyphens, apostrophes, and periods"
                            :min-length "Name must be at least 2 characters"}
                 :ui {:placeholder "Enter your full name"
                      :autocomplete "name"}}}]
]}
```

## 2) Backend
- Shared code builds Malli validators from the metadata (`app.shared.validation.builder`).
- Use the generated validators in handlers/services to return clear error maps/messages.

## 3) Admin UI
- Field specs derived from the same metadata drive input types, placeholders, and client-side checks.
- Render forms using the shared specs so backend and UI stay aligned.

## Common patterns
- **Email**: `:type :email`, regex + max length, `input-type "email"`.
- **Text**: `:type :text`, `:min-length`/`:max-length`, regex for allowed chars.
- **Number**: `:type :number`, `:min-value`/`:max-value`, `:step` for inputs.
- **URL/Phone**: `:type :url` / `:type :phone` with appropriate messages and input types.
- **Enum**: `:type :enum` with `:values [...]`.

## Tips
- Start with high-traffic fields (email/name/password) and expand gradually.
- Keep messages short and user-friendly.
- Reuse shared patterns from `app.shared.patterns` to avoid drift.
- Validation metadata is optional—fields without it still work but won’t get enhanced UX/errors.
