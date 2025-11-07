import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, ButtonGroup, Spinner, Badge } from 'react-bootstrap';
import { toast } from 'sonner';
import { CSVLink } from 'react-csv';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import reportService from '../../services/reportService';


const REGIONS = ['BANGALORE', 'CHENNAI', 'DELHI', 'MUMBAI', 'HYDERABAD', 'KOLKATA'];
const DEVICE_TYPES = ['ROUTER', 'SWITCH', 'FIREWALL', 'LOAD_BALANCER', 'BASE_STATION'];
const LIFECYCLES = ['ACTIVE', 'MAINTENANCE', 'OBSOLETE', 'DECOMMISSIONED'];
const STATUSES = ['ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'NEAR_CAPACITY'];


const StatusBadge = ({ status }) => {
    if (!status) {
        return <Badge bg="secondary">UNKNOWN</Badge>;
    }
    const colors = {
        ACTIVE: 'success',
        EXPIRING_SOON: 'warning',
        EXPIRED: 'danger',
        NEAR_CAPACITY: 'info',
        // For device lifecycle
        MAINTENANCE: 'info',
        OBSOLETE: 'secondary',
        DECOMMISSIONED: 'dark'
    };
    return <Badge bg={colors[status] || 'primary'}>{String(status).replace(/_/g, ' ')}</Badge>;
};



const ReportList = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [reportType, setReportType] = useState('licenses');
    const [reportData, setReportData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({});


    const reportConfigs = {
        licenses: {
            title: 'Licenses',
            api: (f) => reportService.getLicenseReport(f.vendor, f.software, f.region, f.status),
            filters: [
                { name: 'vendor', label: 'Vendor', type: 'text', placeholder: 'e.g., Cisco' },
                { name: 'software', label: 'Software', type: 'text', placeholder: 'e.g., IOS XR' },
                { name: 'region', label: 'Region', type: 'select', options: REGIONS },
                { name: 'status', label: 'Status', type: 'select', options: STATUSES },
            ],
            columns: [
                { key: 'licenseKey', header: 'License Key' },
                { key: 'softwareName', header: 'Software' },
                { key: 'licenseType', header: 'Type' },
                { key: 'usage', header: 'Usage', render: (item) => `${item.currentUsage}/${item.maxUsage}` },
                { key: 'validTo', header: 'Expiry' },
                { key: 'status', header: 'Status', render: (item) => <StatusBadge status={item.status} /> },
                { key: 'region', header: 'Region', render: (item) => <Badge bg="info">{item.region}</Badge> },
                { key: 'vendorName', header: 'Vendor' },
            ]
        },
        devices: {
            title: 'Devices',
            api: (f) => reportService.getDeviceReport(f.deviceType, f.region, f.lifecycle),
            filters: [
                { name: 'deviceType', label: 'Device Type', type: 'select', options: DEVICE_TYPES },
                { name: 'region', label: 'Region', type: 'select', options: REGIONS },
                { name: 'lifecycle', label: 'Lifecycle', type: 'select', options: LIFECYCLES },
            ],
            columns: [
                { key: 'deviceIdName', header: 'Device ID' },
                { key: 'deviceType', header: 'Type' },
                { key: 'model', header: 'Model' },
                { key: 'ipAddress', header: 'IP Address' },
                { key: 'location', header: 'Location' },
                { key: 'region', header: 'Region', render: (item) => <Badge bg="info">{item.region}</Badge> },
                { key: 'lifecycle', header: 'Status', render: (item) => <StatusBadge status={item.lifecycle} /> },
                { key: 'assignedLicensesCount', header: 'Licenses', render: (item) => <Badge bg="primary">{item.assignedLicensesCount}</Badge> },
            ]
        },
        assignments: {
            title: 'Assignments',
            api: (f) => reportService.getAssignmentReport(f.region, f.active ? f.active === 'true' : null),
            filters: [
                { name: 'region', label: 'Region', type: 'select', options: REGIONS },
                { name: 'active', label: 'Status', type: 'select', options: { true: 'Active', false: 'Revoked' } },
            ],
            columns: [
                { key: 'licenseKey', header: 'License Key' },
                { key: 'softwareName', header: 'Software' },
                { key: 'deviceId', header: 'Device ID' },
                { key: 'location', header: 'Location' },
                { key: 'region', header: 'Region', render: (item) => <Badge bg="info">{item.region}</Badge> },
                { key: 'assignedOn', header: 'Assigned On', render: (item) => new Date(item.assignedOn).toLocaleDateString() },
                { key: 'active', header: 'Status', render: (item) => item.active ? <Badge bg="success">Active</Badge> : <Badge bg="secondary">Revoked</Badge> },
            ]
        },
        compliance: {
            title: 'Compliance',
            api: () => reportService.getComplianceReport(),
            filters: [], // No filters for this report
            columns: [
                { key: 'region', header: 'Region' },
                { key: 'totalDevices', header: 'Total Devices' },
                { key: 'devicesWithLicenses', header: 'With Licenses', render: (item) => <Badge bg="success">{item.devicesWithLicenses}</Badge> },
                { key: 'devicesWithoutLicenses', header: 'Without Licenses', render: (item) => <Badge bg="danger">{item.devicesWithoutLicenses}</Badge> },
                { key: 'compliancePercentage', header: 'Compliance %', render: (item) => `${item.compliancePercentage}%` },
                { key: 'expiringLicenses', header: 'Expiring Licenses', render: (item) => <Badge bg="warning">{item.expiringLicenses}</Badge> },
            ]
        }
    };
    
    const currentConfig = reportConfigs[reportType];

    const fetchReport = useCallback(async () => {
        setLoading(true);
        try {
            const data = await currentConfig.api(filters);
            setReportData(data || []);
        } catch (error) {
            console.error('Error fetching report:', error);
            toast.error('Failed to fetch report');
        } finally {
            setLoading(false);
        }
    }, [reportType, filters, currentConfig]);

    useEffect(() => {
        // Reset filters when report type changes and fetch data
        setFilters({});
        fetchReport();
    }, [reportType]); // fetchReport is now stable due to useCallback

    const handleApplyFilters = () => {
        fetchReport();
    };
    
    const resetFilters = () => {
        setFilters({});
    };

    // --- Export Logic (Remains complex but is now driven by config) ---
    const exportToPDF = () => {
        const doc = new jsPDF('landscape');
        doc.setFontSize(18);
        doc.text(`${currentConfig.title.toUpperCase()} REPORT`, 14, 20);
        doc.setFontSize(11);
        doc.text(`Generated on: ${new Date().toLocaleString()}`, 14, 28);

        const headers = [currentConfig.columns.map(col => col.header)];
        const body = reportData.map(item => 
            currentConfig.columns.map(col => item[col.key] ?? '')
        );

        autoTable(doc, { head: headers, body: body, startY: 40 });
        doc.save(`${reportType}_report.pdf`);
        toast.success('PDF exported successfully!');
    };
    
    const getCSVData = () => {
        if (reportData.length === 0) return [];
        const headers = currentConfig.columns.map(col => col.header);
        const data = reportData.map(item => {
            const row = {};
            currentConfig.columns.forEach(col => {
                row[col.header] = item[col.key] ?? '';
            });
            return row;
        });
        return [headers, ...data.map(Object.values)];
    };


    return (
        <>
            <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
            <div className="d-flex">
                <Sidebar />
                <div className="flex-grow-1 p-4" style={{ backgroundColor: '#f8f9fa' }}>
                    <Container fluid>
                        {/* Header */}
                        <Row className="mb-4 align-items-center">
                            <Col>
                                <h2 className="fw-bold"><i className="bi bi-file-earmark-bar-graph me-2"></i>Reports & Export</h2>
                            </Col>
                            <Col xs="auto">
                                <ButtonGroup>
                                    <CSVLink
                                        data={getCSVData()}
                                        filename={`${reportType}_report.csv`}
                                        className="btn btn-success btn-sm"
                                        onClick={() => reportData.length > 0 && toast.success('CSV exported!')}
                                    >
                                        <i className="bi bi-filetype-csv me-2"></i>Export CSV
                                    </CSVLink>
                                    <Button variant="danger" size="sm" onClick={exportToPDF} disabled={reportData.length === 0}>
                                        <i className="bi bi-filetype-pdf me-2"></i>Export PDF
                                    </Button>
                                </ButtonGroup>
                            </Col>
                        </Row>

                        {/* Report Type Selection */}
                        <Card className="shadow-sm border-0 mb-3">
                            <Card.Body>
                                <Form.Label><strong>Select Report Type</strong></Form.Label>
                                <ButtonGroup className="w-100">
                                    {Object.keys(reportConfigs).map(key => (
                                        <Button
                                            key={key}
                                            variant={reportType === key ? 'primary' : 'outline-primary'}
                                            onClick={() => setReportType(key)}
                                        >
                                            {reportConfigs[key].title}
                                        </Button>
                                    ))}
                                </ButtonGroup>
                            </Card.Body>
                        </Card>

                        {/* Filters Card */}
                        {currentConfig.filters.length > 0 && (
                            <ReportFilters
                                filters={filters}
                                config={currentConfig.filters}
                                onChange={setFilters}
                                onApply={handleApplyFilters}
                                onReset={resetFilters}
                            />
                        )}

                        {/* Report Data Card */}
                        <Card className="shadow-sm border-0">
                            <Card.Header>
                                <h5 className="mb-0">
                                    {currentConfig.title} Report
                                    {reportData.length > 0 && <Badge bg="primary" className="ms-2">{reportData.length} records</Badge>}
                                </h5>
                            </Card.Header>
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5"><Spinner animation="border" variant="primary" /><p className="mt-3">Loading...</p></div>
                                ) : reportData.length > 0 ? (
                                    <ReportTable columns={currentConfig.columns} data={reportData} />
                                ) : (
                                    <div className="text-center py-5"><i className="bi bi-inbox text-muted fs-1"></i><p className="text-muted mt-2">No data available</p></div>
                                )}
                            </Card.Body>
                        </Card>

                    </Container>
                </div>
            </div>
            <ChangePasswordModal show={showPasswordModal} onHide={() => setShowPasswordModal(false)} />
        </>
    );
};


const ReportFilters = ({ filters, config, onChange, onApply, onReset }) => {
    const handleFilterChange = (e) => {
        onChange({ ...filters, [e.target.name]: e.target.value });
    };

    return (
        <Card className="shadow-sm border-0 mb-3">
            <Card.Header><h5 className="mb-0"><i className="bi bi-funnel me-2"></i>Filters</h5></Card.Header>
            <Card.Body>
                <Row className="g-3">
                    {config.map(filter => (
                        <Col md={3} key={filter.name}>
                            <Form.Group>
                                <Form.Label>{filter.label}</Form.Label>
                                {filter.type === 'select' ? (
                                    <Form.Select name={filter.name} value={filters[filter.name] || ''} onChange={handleFilterChange}>
                                        <option value="">All</option>
                                        {Array.isArray(filter.options) 
                                            ? filter.options.map(opt => <option key={opt} value={opt}>{opt.replace(/_/g, ' ')}</option>)
                                            : Object.entries(filter.options).map(([val, label]) => <option key={val} value={val}>{label}</option>)
                                        }
                                    </Form.Select>
                                ) : (
                                    <Form.Control
                                        type="text"
                                        name={filter.name}
                                        placeholder={filter.placeholder}
                                        value={filters[filter.name] || ''}
                                        onChange={handleFilterChange}
                                    />
                                )}
                            </Form.Group>
                        </Col>
                    ))}
                    <Col md={3} className="d-flex align-items-end gap-2">
                        <Button variant="primary" onClick={onApply}>Apply</Button>
                        <Button variant="outline-secondary" onClick={onReset}>Reset</Button>
                    </Col>
                </Row>
            </Card.Body>
        </Card>
    );
};


const ReportTable = ({ columns, data }) => (
    <div className="table-responsive">
        <Table hover>
            <thead className="table-light">
                <tr>
                    {columns.map(col => <th key={col.key}>{col.header}</th>)}
                </tr>
            </thead>
            <tbody>
                {data.map((item, index) => (
                    <tr key={index}>
                        {columns.map(col => (
                            <td key={col.key}>
                                {col.render ? col.render(item) : item[col.key]}
                            </td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </Table>
    </div>
);


export default ReportList;