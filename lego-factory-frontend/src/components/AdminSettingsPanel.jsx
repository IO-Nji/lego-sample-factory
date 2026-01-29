import { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import api from '../../api/api';
import { Button } from '../../components';
import '../../styles/AdminSettingsPanel.css';

/**
 * AdminSettingsPanel Component
 * 
 * Provides UI for administrators to view and modify system configuration values.
 * Currently supports:
 * - LOT_SIZE_THRESHOLD: Scenario 4 threshold for direct production
 * 
 * @param {Function} onNotify - Callback to display notifications
 */
function AdminSettingsPanel({ onNotify }) {
  const [settings, setSettings] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  
  // Editable values (local state before saving)
  const [editValues, setEditValues] = useState({});

  // Fetch all editable settings
  const fetchSettings = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch Scenario 4 threshold specifically
      const thresholdResponse = await api.get('/config/scenario4/threshold');
      
      const fetchedSettings = {
        LOT_SIZE_THRESHOLD: {
          key: 'LOT_SIZE_THRESHOLD',
          value: thresholdResponse.data.threshold, // API returns { threshold: N, key: "...", description: "..." }
          description: thresholdResponse.data.description || 'Minimum order quantity to trigger Scenario 4 (direct production).',
          category: 'SCENARIO_4',
          valueType: 'INTEGER'
        }
      };
      
      setSettings(fetchedSettings);
      setEditValues({
        LOT_SIZE_THRESHOLD: fetchedSettings.LOT_SIZE_THRESHOLD.value
      });
    } catch (err) {
      console.error('Failed to fetch settings:', err);
      setError('Failed to load settings: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  // Handle value change in input field
  const handleValueChange = (key, value) => {
    setEditValues(prev => ({
      ...prev,
      [key]: value
    }));
  };

  // Save a specific setting
  const handleSave = async (key) => {
    const newValue = editValues[key];
    const setting = settings[key];
    
    if (!setting) return;
    
    // Validate based on value type
    if (setting.valueType === 'INTEGER') {
      const intValue = parseInt(newValue, 10);
      if (isNaN(intValue) || intValue < 1) {
        setError(`${key} must be a positive integer`);
        return;
      }
    }
    
    try {
      setSaving(true);
      setError(null);
      
      // Use the specific Scenario 4 threshold endpoint
      if (key === 'LOT_SIZE_THRESHOLD') {
        await api.put('/config/scenario4/threshold', {
          threshold: parseInt(newValue, 10)  // API expects { threshold: N }
        });
      } else {
        // Generic config update
        await api.put(`/config/${key}`, {
          value: String(newValue)
        });
      }
      
      // Update local state
      setSettings(prev => ({
        ...prev,
        [key]: {
          ...prev[key],
          value: newValue
        }
      }));
      
      if (onNotify) {
        onNotify(`${key} updated to ${newValue}`, 'success');
      }
    } catch (err) {
      console.error('Failed to save setting:', err);
      setError('Failed to save setting: ' + (err.response?.data?.message || err.message));
      if (onNotify) {
        onNotify('Failed to save setting', 'error');
      }
    } finally {
      setSaving(false);
    }
  };

  // Check if value has been modified
  const isModified = (key) => {
    return settings[key] && String(editValues[key]) !== String(settings[key].value);
  };

  if (loading) {
    return (
      <div className="admin-settings-panel">
        <h3 className="settings-panel-title">⚙️ System Settings</h3>
        <div className="settings-loading">Loading settings...</div>
      </div>
    );
  }

  return (
    <div className="admin-settings-panel">
      <h3 className="settings-panel-title">⚙️ System Settings</h3>
      
      {error && (
        <div className="settings-error">
          {error}
          <button className="error-dismiss" onClick={() => setError(null)}>×</button>
        </div>
      )}
      
      <div className="settings-list">
        {/* Scenario 4 Threshold Setting */}
        {settings.LOT_SIZE_THRESHOLD && (
          <div className="setting-item">
            <div className="setting-header">
              <span className="setting-label">Scenario 4 Threshold</span>
              <span className="setting-category">SCENARIO_4</span>
            </div>
            <p className="setting-description">
              {settings.LOT_SIZE_THRESHOLD.description}
            </p>
            <div className="setting-input-row">
              <input
                type="number"
                min="1"
                value={editValues.LOT_SIZE_THRESHOLD || ''}
                onChange={(e) => handleValueChange('LOT_SIZE_THRESHOLD', e.target.value)}
                className="setting-input"
                aria-label="Lot Size Threshold"
              />
              <Button
                variant="primary"
                size="small"
                onClick={() => handleSave('LOT_SIZE_THRESHOLD')}
                disabled={saving || !isModified('LOT_SIZE_THRESHOLD')}
              >
                {saving ? 'Saving...' : 'Save'}
              </Button>
            </div>
            {isModified('LOT_SIZE_THRESHOLD') && (
              <span className="setting-modified-indicator">Modified (unsaved)</span>
            )}
          </div>
        )}
      </div>
      
      <div className="settings-footer">
        <Button
          variant="secondary"
          size="small"
          onClick={fetchSettings}
          disabled={loading}
        >
          ⟳ Refresh Settings
        </Button>
      </div>
    </div>
  );
}

AdminSettingsPanel.propTypes = {
  onNotify: PropTypes.func
};

export default AdminSettingsPanel;
