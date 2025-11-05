import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import NavigationBar from '../common/Navbar';
import RoleBasedAccess from '../common/RoleBasedAccess';
import Sidebar from '../common/Sidebar';
import licenseService from '../../services/licenseService';
import LicenseFilters from './LicenseFilters';
import LicenseTable from './LicenseTable';

const LicenseList = () => {
  const [licenses, setLicenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [filters, setFilters] = useState({
    search: '',
    region: '',
    licenseType: '',
    active: ''
  });

  const navigate = useNavigate();

  useEffect(() => {
    fetchLicenses();
  }, [currentPage, pageSize]);

  const fetchLicenses = async () => {
    setLoading(true);
    try {
      const response = await licenseService.getAllLicenses(currentPage, pageSize);
      setLicenses(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Error fetching licenses:', error);
    } finally {
      setLoading(false);
    }
  };

  const getUsageVariant = (currentUsage, maxUsage) => {
    const percentage = (currentUsage / maxUsage) * 100;
    if (percentage >= 90) return 'danger';
    if (percentage >= 70) return 'warning';
    return 'success';
  };

  const isExpired = (validTo) => {
    return new Date(validTo) < new Date();
  };

  const isExpiringSoon = (validTo) => {
    const today = new Date();
    const expiryDate = new Date(validTo);
    const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 30 && daysUntilExpiry > 0;
  };

  const isExpiredOrExpiringSoon = (validTo) => {
    return isExpired(validTo) || isExpiringSoon(validTo);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this license?')) {
      try {
        await licenseService.deleteLicense(id);
        fetchLicenses();
      } catch (error) {
        console.error('Error deleting license:', error);
        alert(error.response?.data?.message || 'Failed to delete license');
      }
    }
  };

  const handlePageChange = (page) => setCurrentPage(page);

  const filteredLicenses = licenses.filter((license) => {
    const matchesSearch =
      license.licenseKey?.toLowerCase().includes(filters.search.toLowerCase()) ||
      license.softwareName?.toLowerCase().includes(filters.search.toLowerCase());
    const matchesRegion = !filters.region || license.region === filters.region;
    const matchesType = !filters.licenseType || license.licenseType === filters.licenseType;
    const matchesActive = filters.active === '' || license.active.toString() === filters.active;

    return matchesSearch && matchesRegion && matchesType && matchesActive;
  });

  return (
    <>
      <NavigationBar />
      <div className="d-flex">
        <Sidebar />
        <div className="flex-grow-1 p-4" style={{ backgroundColor: '#f8f9fa' }}>
          <Container fluid>
            <Row className="mb-4 align-items-center">
              <Col xs={12} md={6}>
                <h2 className="fw-bold">
                  <i className="bi bi-key me-2"></i>License Management
                </h2>
              </Col>
              <Col xs={12} md={6} className="text-md-end mt-3 mt-md-0">
              <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER']}>
                <Button variant="primary" onClick={() => navigate('/licenses/new')}>
                  <i className="bi bi-plus-circle me-2"></i>Add License
                </Button>
                </RoleBasedAccess>
              </Col>
            </Row>

            <LicenseFilters
              filters={filters}
              setFilters={setFilters}
              pageSize={pageSize}
              setPageSize={setPageSize}
              setCurrentPage={setCurrentPage}
            />

            <Card className="shadow-sm border-0">
              <Card.Body>
                {loading ? (
                  <div className="text-center py-5">
                    <Spinner animation="border" variant="primary" />
                    <p className="mt-3">Loading licenses...</p>
                  </div>
                ) : (
                  <LicenseTable
                    licenses={filteredLicenses}
                    currentPage={currentPage}
                    totalPages={totalPages}
                    onPageChange={handlePageChange}
                    onDelete={handleDelete}
                    navigate={navigate}
                  />
                )}
              </Card.Body>
            </Card>
          </Container>
        </div>
      </div>
    </>
  );
};

export default LicenseList;
