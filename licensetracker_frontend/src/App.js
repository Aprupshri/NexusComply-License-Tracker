import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Toaster } from 'sonner';
import Login from './components/auth/Login';
import ReportList from './components/reports/ReportList';
import PrivateRoute from './components/auth/PrivateRoute';
import Dashboard from './components/dashboard/Dashboard';
import AuditLogList from './components/auditlogs/AuditLogList';
import DeviceList from './components/devices/DeviceList';
import DeviceForm from './components/devices/DeviceForm';
import VendorForm from './components/vendors/VendorForm';
import AIChat from './components/ai/AIChat';
import VendorList from './components/vendors/VendorList';
import DeviceDetail from './components/devices/DeviceDetail';
import SoftwareVersionList from './components/softwareversions/SoftwareVersionList';
import SoftwareVersionForm from './components/softwareversions/SoftwareVersionForm';
import LicenseList from './components/licenses/LicenseList';
import LicenseForm from './components/licenses/LicenseForm';
import ResetPassword from './components/auth/ResetPassword';
import ForgotPassword from './components/auth/ForgotPassword';
import LicenseDetail from './components/licenses/LicenseDetail';
import AssignmentList from './components/assignments/AssignmentList';
import AlertList from './components/alerts/AlertList';
import UserList from './components/User/UserList';
import UserForm from './components/User/UserForm';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import VanillaDashboard from './components/dashboard/VanillaDashboard';

function App() {
    return (
        <AuthProvider>
            <Router>
                <Toaster
                    position="top-right"
                    richColors
                    closeButton
                    expand={true}
                    duration={3000}
                />

                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/reset-password" element={<ResetPassword />} />
                    
                    <Route
                        path="/dashboard"
                        element={
                            <PrivateRoute>
                                <Dashboard />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/vanilladashboard"
                        element={
                            <PrivateRoute>
                                <VanillaDashboard />
                            </PrivateRoute>
                        }
                    />

                    {/* Device Routes */}
                    <Route
                        path="/devices"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','OPERATIONS_MANAGER']}>
                                <DeviceList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/devices/new"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','OPERATIONS_MANAGER']}>
                                <DeviceForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/devices/edit/:id"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','OPERATIONS_MANAGER']}>
                                <DeviceForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/devices/:id"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','OPERATIONS_MANAGER']}>
                                <DeviceDetail />
                            </PrivateRoute>
                        }
                    />

                    {/* License Routes */}
                    <Route
                        path="/licenses"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','PROCUREMENT_OFFICER']}>
                                <LicenseList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/licenses/new"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','PROCUREMENT_OFFICER']}>
                                <LicenseForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/licenses/edit/:id"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','PROCUREMENT_OFFICER']}>
                                <LicenseForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/licenses/:id"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','NETWORK_ADMIN','PROCUREMENT_OFFICER']}>
                                <LicenseDetail />
                            </PrivateRoute>
                        }
                    />

                    {/* Assignment Routes */}
                    <Route
                        path="/assignments"
                        element={
                            <PrivateRoute allowedRoles={ ['ADMIN','NETWORK_ADMIN']}>
                                <AssignmentList />
                            </PrivateRoute>
                        }
                    />

                    {/* Software Version Routes */}
                    <Route
                        path="/software-versions"
                        element={
                            <PrivateRoute allowedRoles={["ADMIN","OPERATIONS_MANAGER","NETWORK_ENGINEER"]}>
                                <SoftwareVersionList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/software-versions/new"
                        element={
                            <PrivateRoute allowedRoles={["ADMIN","OPERATIONS_MANAGER","NETWORK_ENGINEER"]}>
                                <SoftwareVersionForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/software-versions/edit/:id"
                        element={
                            <PrivateRoute allowedRoles={["ADMIN","OPERATIONS_MANAGER","NETWORK_ENGINEER"]}>
                                <SoftwareVersionForm />
                            </PrivateRoute>
                        }
                    />

                    {/* Alert Routes */}
                    <Route
                        path="/alerts"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER']}>
                                <AlertList />
                            </PrivateRoute>
                        }
                    />

                    <Route
                        path="/users"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','SECURITY_HEAD']}>
                                <UserList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/users/new"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','SECURITY_HEAD']}>
                                <UserForm />
                            </PrivateRoute>
                        }
                    />

                    {/* Report Routes */}
                    <Route
                        path="/reports"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','COMPLIANCE_OFFICER','PROCUREMENT_OFFICER','COMPLIANCE_LEAD','PROCUREMENT_LEAD','SECURITY_HEAD']}>
                                <ReportList />
                            </PrivateRoute>
                        }
                    />

                    {/* Vendor Routes */}
                    <Route
                        path="/vendors"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN','PROCUREMENT_OFFICER','PROCURMENT_LEAD'] }>
                                <VendorList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/vendors/new"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD']}>
                                <VendorForm />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/vendors/edit/:id"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD']}>
                                <VendorForm />
                            </PrivateRoute>
                        }
                    />
                    {/* Audit Log Routes */}
                    <Route
                        path="/audit-logs"
                        element={
                            <PrivateRoute allowedRoles={['ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR']}>
                                <AuditLogList />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/ai-chat"
                        element={<PrivateRoute allowedRoles={["ADMIN","COMPLIANCE_OFFICER","IT_AUDITOR","COMPLIANCE_LEAD","PROCUREMENT_LEAD","PRODUCT_OWNER"]}><AIChat /></PrivateRoute>}
                    />

                    <Route path="/" element={<Navigate to="/vanilladashboard" />} />
                    <Route path="*" element={<Navigate to="/vanilladashboard" />} />
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;
