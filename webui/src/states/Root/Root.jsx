import {Navigate, Outlet} from "react-router-dom";
import {TokenContext} from "@/common/contexts/Token";
import {useContext, useState} from "react";
import {Box, Toolbar} from "@mui/material";
import Sidebar from "@/states/Root/components/Sidebar";
import Header from "@/states/Root/components/Header";

export const Root = () => {

    const [mobileOpen, setMobileOpen] = useState(false);
    const {tokenValid} = useContext(TokenContext);

    return (
        <>
            {tokenValid === false && <Navigate to="/login" />}

            {tokenValid && <Box sx={{ display: 'flex', overflow: 'hidden'}}>
                <Header mobileOpen={mobileOpen} toggleOpen={() => setMobileOpen(current => !current)} />
                <Sidebar mobileOpen={mobileOpen} toggleOpen={() => setMobileOpen(current => !current)} />
                <Box component="main" sx={{ flexGrow: 1, p: 3, ml: { sm: "240px"} }}>
                    <Toolbar />
                    <Outlet />
                </Box>
            </Box>}
        </>
    )
}