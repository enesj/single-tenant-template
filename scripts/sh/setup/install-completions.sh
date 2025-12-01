#!/bin/bash
set -e

echo "🔧 Installing bb task autocompletion..."


# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPLETIONS_DIR="$SCRIPT_DIR"
# Detect shell
if [[ "$SHELL" == *"fish"* ]]; then
    echo "📟 Installing Fish completion..."
    echo "📟 Generating dynamic Fish completions..."
    if [ -f "$COMPLETIONS_DIR/update-fish-completions.sh" ]; then
        "$COMPLETIONS_DIR/update-fish-completions.sh"
    else
        echo "❌ Fish completion generator not found at: $COMPLETIONS_DIR/update-fish-completions.sh"
        exit 1
    fi
    if [ -f "$COMPLETIONS_DIR/bb-completion.fish" ]; then
        cp "$COMPLETIONS_DIR/bb-completion.fish" ~/.config/fish/completions/bb.fish
        echo "✅ Dynamic Fish completion installed!"
        echo "💡 Restart your shell or run: source ~/.config/fish/config.fish"
    else
        echo "❌ Failed to generate Fish completion file"
        exit 1
    fi

elif [[ "$SHELL" == *"bash"* ]]; then
    echo "🐚 Installing Bash completion..."

    # Try different locations for bash completion
    if [[ -d "/usr/local/etc/bash_completion.d" ]]; then
        # Homebrew bash completion directory
        sudo cp "$COMPLETIONS_DIR/bb-completion.bash" /usr/local/etc/bash_completion.d/bb
        echo "✅ Bash completion installed to /usr/local/etc/bash_completion.d/"
    elif [[ -d "/etc/bash_completion.d" ]]; then
        # System bash completion directory
        sudo cp "$COMPLETIONS_DIR/bb-completion.bash" /etc/bash_completion.d/bb
        echo "✅ Bash completion installed to /etc/bash_completion.d/"
    else
        # User-specific installation
        mkdir -p ~/.bash_completion.d
        cp "$COMPLETIONS_DIR/bb-completion.bash" ~/.bash_completion.d/bb

        # Add to .bashrc if not already there
        if ! grep -q "~/.bash_completion.d/bb" ~/.bashrc 2>/dev/null; then
            echo "source ~/.bash_completion.d/bb" >> ~/.bashrc
            echo "✅ Bash completion installed to ~/.bash_completion.d/"
            echo "💡 Added to ~/.bashrc - restart your shell or run: source ~/.bashrc"
        fi
    fi

elif [[ "$SHELL" == *"zsh"* ]]; then
    echo "🔩 Installing Zsh completion..."
    echo "📟 Generating dynamic Zsh completions..."
    if [ -f "$COMPLETIONS_DIR/update-zsh-completions.sh" ]; then
        "$COMPLETIONS_DIR/update-zsh-completions.sh"
    else
        echo "❌ Zsh completion generator not found at: $COMPLETIONS_DIR/update-zsh-completions.sh"
        exit 1
    fi

    # Check if oh-my-zsh is installed
    if [[ -d "$HOME/.oh-my-zsh" ]]; then
        mkdir -p ~/.oh-my-zsh/completions
        if [ -f "$COMPLETIONS_DIR/_bb-completion.zsh" ]; then
            cp "$COMPLETIONS_DIR/_bb-completion.zsh" ~/.oh-my-zsh/completions/_bb
            echo "✅ Dynamic Zsh completion installed to oh-my-zsh!"
        else
            echo "❌ Failed to generate Zsh completion file"
            exit 1
        fi
    else
        # Standard zsh completion
        mkdir -p ~/.zsh/completions
        if [ -f "$COMPLETIONS_DIR/_bb-completion.zsh" ]; then
            cp "$COMPLETIONS_DIR/_bb-completion.zsh" ~/.zsh/completions/_bb
            echo "✅ Dynamic Zsh completion installed to ~/.zsh/completions/"
        else
            echo "❌ Failed to generate Zsh completion file"
            exit 1
        fi

        # Add to .zshrc if not already there
        if ! grep -q "~/.zsh/completions" ~/.zshrc 2>/dev/null; then
            echo "fpath=(~/.zsh/completions \$fpath)" >> ~/.zshrc
            echo "autoload -U compinit && compinit" >> ~/.zshrc
            echo "✅ Zsh completion installed to ~/.zsh/completions/"
            echo "💡 Added to ~/.zshrc - restart your shell or run: source ~/.zshrc"
        fi
    fi

else
    echo "❓ Unknown shell: $SHELL"
    echo "💡 Manual installation:"
    echo "   - Bash: source "$COMPLETIONS_DIR/bb-completion.bash""
    echo "   - Fish: cp "$COMPLETIONS_DIR/bb-completion.fish" ~/.config/fish/completions/bb.fish"
fi

echo ""
echo "💡 Dynamic completions: Fish and Zsh completions are generated from actual bb tasks and scripts"
echo ""
echo "🎉 Autocompletion features:"
echo "   • Tab complete bb tasks: bb <TAB>"
echo "   • Complete test scripts: bb script <TAB>"
echo "   • Complete library names: bb single-dep-upgrade <TAB>"
echo "   • Complete command options: bb commit --<TAB>"
echo ""
echo "📖 Try typing: bb build-<TAB>"

# Add update command for manual completion refresh
echo ""  # Add blank line for spacing
echo "💡 To refresh completions later, run:"
echo "   ./scripts/sh/setup/update-fish-completions.sh  # For Fish"
echo "   ./scripts/sh/setup/update-zsh-completions.sh   # For Zsh"
