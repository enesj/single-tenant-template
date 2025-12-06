# ADE Extraction Schema — POS Receipt (Totals + Line Items)

This document defines the **LandingAI ADE Extract** schema used to extract:
- Supplier (merchant) data
- Purchase date/time
- Total amount (no subtotals/tax)
- Optional payment hints (for payer auto-suggestion)
- Line items (for article DB + price comparisons)

Use this schema with the ADE **Extract** endpoint or the `landingai-ade` TypeScript library by passing it as the `schema` argument in your Extract request. The Extract API accepts a JSON schema object in the `schema` field. 

---

## Data Model (What We Extract)

### Required
- `supplier.name`
- `total.amount`

### Optional
- `supplier.address`
- `supplier.tax_id`
- `purchased_at`
- `total.currency`
- `payment_hints.method`
- `payment_hints.card_last4`
- `line_items[]` (can be empty, but included for price comparison)

---

## JSON Extraction Schema

> Notes:
> - `purchased_at` allows `date-time`, `date`, or freeform string to handle receipts that don’t format cleanly.
> - `payment_hints` is used to **suggest** a payer; the user can override in UI.
> - `line_items[].raw_label` retains the original receipt wording to support later normalization/matching.

```json
{
  "type": "object",
  "title": "POS Receipt - Totals + Line Items",
  "description": "Extract supplier, purchase datetime, total, optional payment hints, and line items for price comparison.",
  "properties": {
    "supplier": {
      "type": "object",
      "title": "Supplier / Merchant",
      "properties": {
        "name": { "type": "string", "description": "Merchant/store name." },
        "address": { "type": "string", "nullable": true, "description": "Merchant address, if present." },
        "tax_id": { "type": "string", "nullable": true, "description": "Merchant tax ID, if present." }
      },
      "required": ["name"]
    },

    "purchased_at": {
      "title": "Purchase Date/Time",
      "description": "Purchase date and time if available. Prefer ISO 8601. If only date is present, return YYYY-MM-DD.",
      "anyOf": [
        { "type": "string", "format": "date-time" },
        { "type": "string", "format": "date" },
        { "type": "string" }
      ],
      "nullable": true
    },

    "total": {
      "type": "object",
      "title": "Total Amount",
      "properties": {
        "amount": { "type": "number", "minimum": 0, "description": "Total amount paid (gross)." },
        "currency": { "type": "string", "nullable": true, "description": "Currency code if present." }
      },
      "required": ["amount"]
    },

    "payment_hints": {
      "type": "object",
      "title": "Payment Hints",
      "nullable": true,
      "description": "Optional hints used to auto-suggest payer; user can override.",
      "properties": {
        "method": { "type": "string", "nullable": true, "enum": ["cash", "card", "unknown"] },
        "card_last4": { "type": "string", "nullable": true, "description": "Last 4 digits if printed." }
      }
    },

    "line_items": {
      "type": "array",
      "title": "Line Items",
      "description": "Items purchased as printed on the receipt. Use this for article normalization + price comparisons.",
      "minItems": 0,
      "maxItems": 100,
      "items": {
        "type": "object",
        "properties": {
          "raw_label": {
            "type": "string",
            "description": "Item/Article label exactly as printed."
          },
          "qty": {
            "type": "number",
            "nullable": true,
            "minimum": 0,
            "description": "Quantity, if present."
          },
          "unit_price": {
            "type": "number",
            "nullable": true,
            "minimum": 0,
            "description": "Unit price, if present."
          },
          "line_total": {
            "type": "number",
            "minimum": 0,
            "description": "Total price for this line item."
          }
        },
        "required": ["raw_label", "line_total"],
        "propertyOrdering": ["raw_label", "qty", "unit_price", "line_total"]
      }
    }
  },
  "required": ["supplier", "total"],
  "propertyOrdering": ["supplier", "purchased_at", "total", "payment_hints", "line_items"]
}
