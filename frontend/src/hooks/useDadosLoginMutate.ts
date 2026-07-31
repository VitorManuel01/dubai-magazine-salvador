import axios from "axios";
import { useMutation } from "@tanstack/react-query";
import { LoginData } from "../interface/LoginData";
import { useAuth } from "../context/AuthContext";

interface LoginResponse {
    token: string;
}

const postLogin = async (data: LoginData): Promise<LoginResponse> => {
    const response = await axios.post<LoginResponse>("/auth/login", data);
    return response.data;
};

export const useDadosLoginMutate = () => {
    const { login } = useAuth();

    return useMutation({
        mutationFn: postLogin,
        retry: false,
        onSuccess: (data) => {
            login(data.token);
        },
    });
}
