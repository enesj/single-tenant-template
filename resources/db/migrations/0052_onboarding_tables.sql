-- Workspace onboarding tables
-- OnboardingProgress keyed on (user_id, role), not per membership.

-- Enums
CREATE TYPE onboarding_step_kind AS ENUM (
    'name_workspace',
    'setup_profile',
    'configure_categories',
    'review_payer_types',
    'invite_members',
    'management_intro',
    'upload_first_receipt',
    'browse_intro'
);

CREATE TYPE onboarding_step_status AS ENUM (
    'pending',
    'completed',
    'skipped'
);

-- Progress table
CREATE TABLE onboarding_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role membership_role NOT NULL,
    dismissed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX uniq_onboarding_progress_user_role
    ON onboarding_progress (user_id, role);
CREATE INDEX idx_onboarding_progress_user
    ON onboarding_progress (user_id);

-- Steps table
CREATE TABLE onboarding_steps (
    id UUID PRIMARY KEY,
    progress_id UUID NOT NULL REFERENCES onboarding_progress(id) ON DELETE CASCADE,
    kind onboarding_step_kind NOT NULL,
    step_order INTEGER NOT NULL,
    status onboarding_step_status NOT NULL DEFAULT 'pending',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX uniq_onboarding_steps_progress_kind
    ON onboarding_steps (progress_id, kind);
CREATE INDEX idx_onboarding_steps_progress
    ON onboarding_steps (progress_id);
CREATE INDEX idx_onboarding_steps_status
    ON onboarding_steps (status);
