-- ===========================================================================
-- Parental Control App Database Schema (PostgreSQL/Supabase)
-- ===========================================================================
-- NOTE: This file uses PostgreSQL syntax for Supabase.
-- VS Code MSSQL extension may show false errors - IGNORE THEM.
-- This migration has already been successfully applied to Supabase.
-- ===========================================================================

-- Enable UUID extension in the extensions schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA extensions;

-- ============================================
-- USERS TABLE (Parent accounts)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- DEVICES TABLE (Child devices)
-- ============================================
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT UNIQUE NOT NULL,
    parent_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL DEFAULT 'Child Device',
    device_model TEXT,
    android_version TEXT,
    app_version TEXT,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMPTZ,
    battery_level INTEGER DEFAULT 0,
    is_charging BOOLEAN DEFAULT FALSE,
    network_type TEXT DEFAULT 'unknown',
    paired_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- PAIRING CODES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS pairing_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT UNIQUE NOT NULL,
    device_id TEXT NOT NULL,
    device_name TEXT,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for quick code lookups
CREATE INDEX IF NOT EXISTS idx_pairing_codes_code ON pairing_codes(code);
CREATE INDEX IF NOT EXISTS idx_pairing_codes_expires ON pairing_codes(expires_at);

-- ============================================
-- LOCATIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy REAL,
    altitude DOUBLE PRECISION,
    speed REAL,
    bearing REAL,
    provider TEXT,
    address TEXT,
    recorded_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for location queries
CREATE INDEX IF NOT EXISTS idx_locations_device ON locations(device_id);
CREATE INDEX IF NOT EXISTS idx_locations_recorded ON locations(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_locations_device_time ON locations(device_id, recorded_at DESC);

-- ============================================
-- COMMANDS TABLE (Parent to Child)
-- ============================================
CREATE TABLE IF NOT EXISTS commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    command_type TEXT NOT NULL,
    payload JSONB DEFAULT '{}',
    status TEXT DEFAULT 'pending', -- pending, executing, completed, failed
    result JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    executed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

-- Index for pending commands
CREATE INDEX IF NOT EXISTS idx_commands_device ON commands(device_id);
CREATE INDEX IF NOT EXISTS idx_commands_status ON commands(status);
CREATE INDEX IF NOT EXISTS idx_commands_pending ON commands(device_id, status) WHERE status = 'pending';

-- ============================================
-- NOTIFICATIONS TABLE (From child device)
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    title TEXT,
    content TEXT,
    posted_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for notification queries
CREATE INDEX IF NOT EXISTS idx_notifications_device ON notifications(device_id);
CREATE INDEX IF NOT EXISTS idx_notifications_posted ON notifications(posted_at DESC);

-- ============================================
-- APP USAGE TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS app_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    usage_time_ms BIGINT DEFAULT 0,
    foreground_time_ms BIGINT DEFAULT 0,
    last_used TIMESTAMPTZ,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, package_name, usage_date)
);

-- Index for app usage queries
CREATE INDEX IF NOT EXISTS idx_app_usage_device ON app_usage(device_id);
CREATE INDEX IF NOT EXISTS idx_app_usage_date ON app_usage(usage_date);

-- ============================================
-- BLOCKED APPS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS blocked_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT,
    blocked_by UUID REFERENCES users(id),
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, package_name)
);

-- ============================================
-- SCREEN TIME LIMITS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS screen_time_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    daily_limit_minutes INTEGER DEFAULT 120,
    bedtime_start TIME,
    bedtime_end TIME,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id)
);

-- ============================================
-- GEOFENCES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS geofences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_meters REAL NOT NULL DEFAULT 100,
    is_safe_zone BOOLEAN DEFAULT TRUE,
    notify_on_enter BOOLEAN DEFAULT TRUE,
    notify_on_exit BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- GEOFENCE EVENTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS geofence_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    geofence_id UUID REFERENCES geofences(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    event_type TEXT NOT NULL, -- 'enter' or 'exit'
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- SNAPSHOTS TABLE (Screenshots/Camera captures)
-- ============================================
CREATE TABLE IF NOT EXISTS snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    snapshot_type TEXT NOT NULL, -- 'camera' or 'screen'
    image_data TEXT, -- Base64 encoded (for small images)
    storage_path TEXT, -- For Supabase Storage
    thumbnail_path TEXT,
    file_size INTEGER,
    width INTEGER,
    height INTEGER,
    captured_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for snapshot queries
CREATE INDEX IF NOT EXISTS idx_snapshots_device ON snapshots(device_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_captured ON snapshots(captured_at DESC);

-- ============================================
-- CALL LOGS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS call_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    phone_number TEXT,
    contact_name TEXT,
    call_type TEXT NOT NULL, -- 'incoming', 'outgoing', 'missed'
    duration_seconds INTEGER DEFAULT 0,
    call_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for call log queries
CREATE INDEX IF NOT EXISTS idx_call_logs_device ON call_logs(device_id);
CREATE INDEX IF NOT EXISTS idx_call_logs_time ON call_logs(call_time DESC);

-- ============================================
-- SMS LOGS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS sms_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    phone_number TEXT,
    contact_name TEXT,
    message_type TEXT NOT NULL, -- 'sent' or 'received'
    body TEXT,
    sms_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for SMS log queries
CREATE INDEX IF NOT EXISTS idx_sms_logs_device ON sms_logs(device_id);
CREATE INDEX IF NOT EXISTS idx_sms_logs_time ON sms_logs(sms_time DESC);

-- ============================================
-- ALERTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL REFERENCES devices(device_id) ON DELETE CASCADE,
    alert_type TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT,
    severity TEXT DEFAULT 'info', -- 'info', 'warning', 'critical'
    is_read BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for alert queries
CREATE INDEX IF NOT EXISTS idx_alerts_device ON alerts(device_id);
CREATE INDEX IF NOT EXISTS idx_alerts_unread ON alerts(device_id, is_read) WHERE is_read = FALSE;

-- ============================================
-- REALTIME SIGNALING TABLE (WebRTC)
-- ============================================
CREATE TABLE IF NOT EXISTS signaling (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    session_id TEXT NOT NULL,
    signal_type TEXT NOT NULL, -- 'offer', 'answer', 'ice-candidate'
    from_role TEXT NOT NULL, -- 'parent' or 'child'
    payload JSONB NOT NULL,
    is_processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT NOW() + INTERVAL '5 minutes'
);

-- Index for signaling queries
CREATE INDEX IF NOT EXISTS idx_signaling_device ON signaling(device_id);
CREATE INDEX IF NOT EXISTS idx_signaling_session ON signaling(session_id);
CREATE INDEX IF NOT EXISTS idx_signaling_unprocessed ON signaling(device_id, is_processed) WHERE is_processed = FALSE;

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE pairing_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocked_apps ENABLE ROW LEVEL SECURITY;
ALTER TABLE screen_time_limits ENABLE ROW LEVEL SECURITY;
ALTER TABLE geofences ENABLE ROW LEVEL SECURITY;
ALTER TABLE geofence_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE call_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE sms_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE signaling ENABLE ROW LEVEL SECURITY;

-- ============================================
-- RLS POLICIES
-- ============================================

-- Users can read/update their own profile
CREATE POLICY "Users can view own profile" ON users
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON users
    FOR UPDATE USING (auth.uid() = id);

-- Parents can manage their devices
CREATE POLICY "Parents can view their devices" ON devices
    FOR SELECT USING (parent_id = auth.uid());

CREATE POLICY "Parents can update their devices" ON devices
    FOR UPDATE USING (parent_id = auth.uid());

CREATE POLICY "Parents can delete their devices" ON devices
    FOR DELETE USING (parent_id = auth.uid());

-- Anyone can create pairing codes (for child app)
CREATE POLICY "Anyone can create pairing codes" ON pairing_codes
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Anyone can read pairing codes" ON pairing_codes
    FOR SELECT USING (true);

CREATE POLICY "Anyone can update pairing codes" ON pairing_codes
    FOR UPDATE USING (true);

-- Device data policies (locations, notifications, etc.)
-- Child devices insert, parents read their children's data
CREATE POLICY "Devices can insert locations" ON locations
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view locations" ON locations
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = locations.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

CREATE POLICY "Devices can insert commands" ON commands
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Devices can read commands" ON commands
    FOR SELECT USING (true);

CREATE POLICY "Devices can update commands" ON commands
    FOR UPDATE USING (true);

CREATE POLICY "Devices can insert notifications" ON notifications
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view notifications" ON notifications
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = notifications.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

CREATE POLICY "Devices can insert app_usage" ON app_usage
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Devices can update app_usage" ON app_usage
    FOR UPDATE USING (true);

CREATE POLICY "Parents can view app_usage" ON app_usage
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = app_usage.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Blocked apps - parents manage
CREATE POLICY "Parents can manage blocked_apps" ON blocked_apps
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = blocked_apps.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Screen time - parents manage
CREATE POLICY "Parents can manage screen_time_limits" ON screen_time_limits
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = screen_time_limits.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Geofences - parents manage
CREATE POLICY "Parents can manage geofences" ON geofences
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = geofences.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Geofence events - devices insert, parents read
CREATE POLICY "Devices can insert geofence_events" ON geofence_events
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view geofence_events" ON geofence_events
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = geofence_events.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Snapshots - devices insert, parents view
CREATE POLICY "Devices can insert snapshots" ON snapshots
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view snapshots" ON snapshots
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = snapshots.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Call logs - devices insert, parents view
CREATE POLICY "Devices can insert call_logs" ON call_logs
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view call_logs" ON call_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = call_logs.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- SMS logs - devices insert, parents view
CREATE POLICY "Devices can insert sms_logs" ON sms_logs
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view sms_logs" ON sms_logs
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = sms_logs.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Alerts - devices insert, parents view
CREATE POLICY "Devices can insert alerts" ON alerts
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Parents can view alerts" ON alerts
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = alerts.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

CREATE POLICY "Parents can update alerts" ON alerts
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM devices 
            WHERE devices.device_id = alerts.device_id 
            AND devices.parent_id = auth.uid()
        )
    );

-- Signaling - open for WebRTC
CREATE POLICY "Anyone can insert signaling" ON signaling
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Anyone can read signaling" ON signaling
    FOR SELECT USING (true);

CREATE POLICY "Anyone can update signaling" ON signaling
    FOR UPDATE USING (true);

CREATE POLICY "Anyone can delete signaling" ON signaling
    FOR DELETE USING (true);

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to update 'updated_at' timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_app_usage_updated_at
    BEFORE UPDATE ON app_usage
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_screen_time_limits_updated_at
    BEFORE UPDATE ON screen_time_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_geofences_updated_at
    BEFORE UPDATE ON geofences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to clean up expired pairing codes
CREATE OR REPLACE FUNCTION cleanup_expired_pairing_codes()
RETURNS void AS $$
BEGIN
    DELETE FROM pairing_codes WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Function to clean up expired signaling
CREATE OR REPLACE FUNCTION cleanup_expired_signaling()
RETURNS void AS $$
BEGIN
    DELETE FROM signaling WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- REALTIME SUBSCRIPTIONS
-- ============================================

-- Enable realtime for tables that need instant updates
ALTER PUBLICATION supabase_realtime ADD TABLE devices;
ALTER PUBLICATION supabase_realtime ADD TABLE commands;
ALTER PUBLICATION supabase_realtime ADD TABLE locations;
ALTER PUBLICATION supabase_realtime ADD TABLE alerts;
ALTER PUBLICATION supabase_realtime ADD TABLE signaling;
