-- Seed Bosnia and Herzegovina country (required before adding cities.country FK with default)
INSERT INTO public.countries (country, code, created_at, updated_at)
VALUES ('Bosnia and Herzegovina', 'BA', NOW(), NOW())
ON CONFLICT (country) DO NOTHING;
